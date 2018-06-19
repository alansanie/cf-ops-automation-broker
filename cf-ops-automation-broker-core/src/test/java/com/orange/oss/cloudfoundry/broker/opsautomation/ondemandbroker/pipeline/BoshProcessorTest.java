package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceBindingService;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.servicebroker.model.*;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_ORGANIZATION_GUID;
import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_SPACE_GUID;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.servicebroker.model.CloudFoundryContext.CLOUD_FOUNDRY_PLATFORM;

public class BoshProcessorTest {

    private static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    private static final String TEMPLATES_REPOSITORY_ALIAS_NAME = "paas-template.";
    private static final String SECRETS_REPOSITORY_ALIAS_NAME = "paas-secrets.";

    @Test
    public void creates_structures_and_returns_async_response() {
        //Given a creation request
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                null
        );
        request.withServiceInstanceId(SERVICE_INSTANCE_ID);
        request.withOriginatingIdentity(aContext());


        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, request);
        context.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour
        TemplatesGenerator templatesGenerator = mock(TemplatesGenerator.class);
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);

        //given a configured timeout
        @SuppressWarnings("unchecked")
        PipelineCompletionTracker tracker = aCompletionTracker();

        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, templatesGenerator, secretsGenerator, tracker, "Cassandra", "c_");

        //When
        boshProcessor.preCreate(context);

        //Then verify parameters and delegation on calls
        verify(templatesGenerator).checkPrerequisites(aGitRepoWorkDir());
        verify(templatesGenerator).generate(eq(aGitRepoWorkDir()), eq(SERVICE_INSTANCE_ID), any(CoabVarsFileDto.class));
        verify(secretsGenerator).checkPrerequisites(aGitRepoWorkDir());
        verify(secretsGenerator).generate(aGitRepoWorkDir(), SERVICE_INSTANCE_ID, null);

        //Then verify populated context
        CreateServiceInstanceResponse serviceInstanceResponse = (CreateServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE);
        // specifying asynchronous creations
        assertThat(serviceInstanceResponse.isAsync()).isTrue();

        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(request, "2017-11-14T17:24:08.007Z");
        String expectedJsonPipelineOperationState = tracker.formatAsJson(pipelineOperationState);

        //when
        assertThat(serviceInstanceResponse.getOperation()).isEqualTo(expectedJsonPipelineOperationState);
         // and with a non-null commit message (asserted in a specific test)
        String customTemplateMessage = (String) context.contextKeys.get(TEMPLATES_REPOSITORY_ALIAS_NAME+GitProcessorContext.commitMessage.toString());
        assertThat(customTemplateMessage).isNotNull();
        String customSecretsMessage = (String) context.contextKeys.get(SECRETS_REPOSITORY_ALIAS_NAME+GitProcessorContext.commitMessage.toString());
        assertThat(customSecretsMessage).isNotNull();
    }

    @Test
    public void constructs_a_dto_from_a_provisionning_request() {
        //Given mocked dependencies
        TemplatesGenerator templatesGenerator = mock(TemplatesGenerator.class);
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);
        PipelineCompletionTracker tracker = aCompletionTracker();

        //Given a basic processor with deployment model
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, templatesGenerator, secretsGenerator, tracker, "Cassandra", "c");


        //Given a creation request with both deprecated and new OSB syntax
        Map<String, Object> contextProperties = new HashMap<>();
        contextProperties.put(OSB_PROFILE_ORGANIZATION_GUID, "org_id1");
        contextProperties.put(OSB_PROFILE_SPACE_GUID, "space_id1");

        Map<String, Object> params = new HashMap<>();
        params.put("a-string-param", "a-string-value");
        params.put("a number param", 24);
        params.put("a boolean param", true);

        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id1",
                "plan_id1",
                "org_id1",
                "space_id1",
                new org.springframework.cloud.servicebroker.model.Context(
                        CLOUD_FOUNDRY_PLATFORM,
                        contextProperties
                ),
                params
        );
        request.withServiceInstanceId("service-instance-id1");
        Map<String, Object> properties = new HashMap<>();
        properties.put(OsbConstants.ORIGINATING_USER_KEY, "user_guid1");
        request.withOriginatingIdentity(new org.springframework.cloud.servicebroker.model.Context(OsbConstants.ORIGINATING_CLOUDFOUNDRY_PLATFORM, properties));

        //when
        CoabVarsFileDto coabVarsFileDto = boshProcessor.wrapOsbIntoVarsDto(request);

        //then
        assertThat(coabVarsFileDto.instance_id).isEqualTo("service-instance-id1");
        assertThat(coabVarsFileDto.plan_id).isEqualTo("plan_id1");
        assertThat(coabVarsFileDto.service_id).isEqualTo("service_definition_id1");
        assertThat(coabVarsFileDto.deployment_name).isEqualTo("c" + "_" + "service-instance-id1");
        assertThat(coabVarsFileDto.context.organization_guid).isEqualTo("org_id1");
        assertThat(coabVarsFileDto.context.space_guid).isEqualTo("space_id1");
        assertThat(coabVarsFileDto.context.user_guid).isEqualTo("user_guid1");
        assertThat(coabVarsFileDto.parameters).isEqualTo(params);
    }


    @Test
    public void provision_commit_msg_includes_requester_details_without_context() {
        //Given a creation request with both deprecated OSB syntax

        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id1",
                "space_id1",
                new org.springframework.cloud.servicebroker.model.Context(
                        CLOUD_FOUNDRY_PLATFORM,
                        null
                ),
                null
        );
        request.withServiceInstanceId(SERVICE_INSTANCE_ID);

        //then commit msg is valid
        provision_commit_msg_includes_requester_details(request);
    }

    @Test
    public void provision_commit_msg_includes_requester_details_with_context() {
        //Given a creation request with both deprecated OSB syntax and new context syntax
        Map<String, Object> contextProperties = new HashMap<>();
        contextProperties.put(OSB_PROFILE_ORGANIZATION_GUID, "org_id1");
        contextProperties.put(OSB_PROFILE_SPACE_GUID, "space_id1");

        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id1",
                "space_id1",
                new org.springframework.cloud.servicebroker.model.Context(
                        CLOUD_FOUNDRY_PLATFORM,
                        contextProperties
                ),
                null
        );
        request.withServiceInstanceId(SERVICE_INSTANCE_ID);

        provision_commit_msg_includes_requester_details(request);
    }

    protected void provision_commit_msg_includes_requester_details(CreateServiceInstanceRequest request) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(OsbConstants.ORIGINATING_USER_KEY, "user_guid1");
        request.withOriginatingIdentity(new org.springframework.cloud.servicebroker.model.Context(OsbConstants.ORIGINATING_CLOUDFOUNDRY_PLATFORM, properties));
        BoshProcessor boshProcessor = aBasicBoshProcessor();


        //When
        assertThat(boshProcessor.formatProvisionCommitMsg(request)).isEqualTo("Cassandra broker: create instance id=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0" +
                "\n\nRequested from space_guid=space_id1 org_guid=org_id1 by user_guid=user_guid1");
    }

    private BoshProcessor aBasicBoshProcessor() {
        //Given mocked dependencies
        TemplatesGenerator templatesGenerator = mock(TemplatesGenerator.class);
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);
        PipelineCompletionTracker tracker = aCompletionTracker();

        return new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, templatesGenerator, secretsGenerator, tracker, "Cassandra", "c");
    }

    @Test
    public void unprovision_commit_msg_includes_requester_details() {
        //Given a delete request
        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest(SERVICE_INSTANCE_ID,
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                false);

        Map<String, Object> properties = new HashMap<>();
        properties.put(OsbConstants.ORIGINATING_USER_KEY, "user_guid1");
        request.withOriginatingIdentity(new org.springframework.cloud.servicebroker.model.Context(OsbConstants.ORIGINATING_CLOUDFOUNDRY_PLATFORM, properties));


        //Given mocked dependencies
        BoshProcessor boshProcessor = aBasicBoshProcessor();

        //When
        assertThat(boshProcessor.formatUnprovisionCommitMsg(request)).isEqualTo("Cassandra broker: delete instance id=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0" +
                "\n\nRequested by user_guid=user_guid1");
    }


    @Test
    public void responds_to_get_last_service_operation_in_progress() {

        //Given a get last operation request (asynchronous polling from Cloud Controller)
        GetLastServiceOperationRequest operationRequest = new GetLastServiceOperationRequest(SERVICE_INSTANCE_ID,
                "service_definition_id",
                "plan_id",
                DeploymentConstants.OSB_OPERATION_CREATE);

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST, operationRequest);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (in progress state)
        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse();
        expectedResponse.withOperationState(OperationState.IN_PROGRESS);
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        when(tracker.getDeploymentExecStatus(any(Path.class), eq(SERVICE_INSTANCE_ID), eq(DeploymentConstants.OSB_OPERATION_CREATE), any(GetLastServiceOperationRequest.class))).thenReturn(expectedResponse);


        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra", "c");
        boshProcessor.preGetLastOperation(context);

        //Then mapped response from tracker is returned
        GetLastServiceOperationResponse operationResponse = (GetLastServiceOperationResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE);
        assertThat(operationResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void delegates_bind_request_to_completion_nested_broker() {

        //given
        CreateServiceInstanceBindingRequest request = OsbBuilderHelper.aBindingRequest("service-instance-id");

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceBindingService.CREATE_SERVICE_INSTANCE_BINDING_REQUEST, request);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (in progress state)
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        when(tracker.delegateBindRequest(any(Path.class), any(CreateServiceInstanceBindingRequest.class))).thenReturn(OsbBuilderHelper.aBindingResponse());


        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra", "c");
        boshProcessor.preBind(context);

        //Then
        verify(tracker).delegateBindRequest(any(Path.class), eq(request));
        //And mapped response from tracker is returned
        CreateServiceInstanceBindingResponse bindingResponse = (CreateServiceInstanceBindingResponse) context.contextKeys.get(ProcessorChainServiceInstanceBindingService.CREATE_SERVICE_INSTANCE_BINDING_RESPONSE);
        assertThat(bindingResponse).isEqualTo(OsbBuilderHelper.aBindingResponse());
    }

    @Test
    public void delegates_unbind_request_to_completion_nested_broker() {
        //given
        DeleteServiceInstanceBindingRequest request = OsbBuilderHelper.anUnbindRequest("service-instance-id", "service-binding-id");

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceBindingService.DELETE_SERVICE_INSTANCE_BINDING_REQUEST, request);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (in progress state)
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        doNothing().when(tracker).delegateUnbindRequest(any(Path.class), any(DeleteServiceInstanceBindingRequest.class));

        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra", "c");
        boshProcessor.preUnBind(context);

        //Then
        verify(tracker).delegateUnbindRequest(any(Path.class), eq(request));
    }


    @Test
    public void responds_to_get_last_service_operation_succeeded() {

        //Given a get last operation request (asynchronous polling from Cloud Controller)
        GetLastServiceOperationRequest operationRequest = new GetLastServiceOperationRequest(SERVICE_INSTANCE_ID,
                "service_definition_id",
                "plan_id",
                DeploymentConstants.OSB_OPERATION_CREATE);

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST, operationRequest);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (succeeded state)
        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse();
        expectedResponse.withDescription("Creation is succeeded");
        expectedResponse.withOperationState(OperationState.SUCCEEDED);
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        when(tracker.getDeploymentExecStatus(any(Path.class), eq(SERVICE_INSTANCE_ID), eq(DeploymentConstants.OSB_OPERATION_CREATE), any(GetLastServiceOperationRequest.class))).thenReturn(expectedResponse);

        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra", "c");
        boshProcessor.preGetLastOperation(context);

        //Then mapped response from tracker is returned
        GetLastServiceOperationResponse operationResponse = (GetLastServiceOperationResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE);
        assertThat(operationResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void unprovision_removes_secrets_structures_and_returns_async_response() {
        //Given a delete request
        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest(SERVICE_INSTANCE_ID,
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                false);
        request.withOriginatingIdentity(aContext());

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST, request);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);

        //given a configured timeout within tracker
        @SuppressWarnings("unchecked")
        PipelineCompletionTracker tracker = aCompletionTracker();


        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, secretsGenerator, tracker, "Cassandra", "c");
        boshProcessor.preDelete(context);

        //Then verify parameters and delegation on calls
        verify(secretsGenerator).checkPrerequisites(aGitRepoWorkDir());
        verify(secretsGenerator).remove(aGitRepoWorkDir(), SERVICE_INSTANCE_ID);

        //Then verify populated context
        DeleteServiceInstanceResponse serviceInstanceResponse = (DeleteServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_RESPONSE);
        // specifying asynchronous creations
        assertThat(serviceInstanceResponse.isAsync()).isTrue();

        //and operation state is specified
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(request, "2017-11-14T17:24:08.007Z");
        String expectedJsonPipelineOperationState = tracker.formatAsJson(pipelineOperationState);

        assertThat(serviceInstanceResponse.getOperation()).isEqualTo(expectedJsonPipelineOperationState);

        // and with a commit message (asserted in a distinct test)
        String customMessage = (String) context.contextKeys.get(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.commitMessage.toString());
        assertThat(customMessage).isNotNull();
    }



    private PipelineCompletionTracker aCompletionTracker() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L), ZoneId.of("Europe/Paris"));
        return new PipelineCompletionTracker(clock, 1200L, Mockito.mock(OsbProxy.class), Mockito.mock(SecretsReader.class));
    }

    private Path aGitRepoWorkDir() {
        return FileSystems.getDefault().getPath("/a/git_workdir/path");
    }

    private org.springframework.cloud.servicebroker.model.Context aContext() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(OsbConstants.ORIGINATING_USER_KEY, "user_guid");
        properties.put(OsbConstants.ORIGINATING_EMAIL_KEY, "user_email");
        return new org.springframework.cloud.servicebroker.model.Context(OsbConstants.ORIGINATING_CLOUDFOUNDRY_PLATFORM, properties);
    }


}