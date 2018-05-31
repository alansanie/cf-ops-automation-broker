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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class BoshProcessorTest {

    private static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    private static final String TEMPLATES_REPOSITORY_ALIAS_NAME = "paas-template.";
    private static final String SECRETS_REPOSITORY_ALIAS_NAME = "paas-secrets.";

    @Test
    public void creates_structures_and_returns_async_response() {
        //Given a creation request
        CreateServiceInstanceRequest creationRequest = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                null
        );
        creationRequest.withServiceInstanceId(SERVICE_INSTANCE_ID);

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, creationRequest);
        context.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour
        TemplatesGenerator templatesGenerator = mock(TemplatesGenerator.class);
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);

        //given a configured timeout
        @SuppressWarnings("unchecked")
        PipelineCompletionTracker tracker = aCompletionTracker();

        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, templatesGenerator, secretsGenerator, tracker, "Cassandra");

        //When
        boshProcessor.preCreate(context);

        //Then verify parameters and delegation on calls
        verify(templatesGenerator).checkPrerequisites(aGitRepoWorkDir());
        verify(templatesGenerator).generate(aGitRepoWorkDir(), SERVICE_INSTANCE_ID);
        verify(secretsGenerator).checkPrerequisites(aGitRepoWorkDir());
        verify(secretsGenerator).generate(aGitRepoWorkDir(), SERVICE_INSTANCE_ID);

        //Then verify populated context
        CreateServiceInstanceResponse serviceInstanceResponse = (CreateServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE);
        // specifying asynchronous creations
        assertThat(serviceInstanceResponse.isAsync()).isTrue();

        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(creationRequest, "2017-11-14T17:24:08.007Z");
        String expectedJsonPipelineOperationState = tracker.formatAsJson(pipelineOperationState);

        //when
        assertThat(serviceInstanceResponse.getOperation()).isEqualTo(expectedJsonPipelineOperationState);
         // and with a proper commit message
        String customTemplateMessage = (String) context.contextKeys.get(TEMPLATES_REPOSITORY_ALIAS_NAME+GitProcessorContext.commitMessage.toString());
        assertThat(customTemplateMessage).isEqualTo("Cassandra broker: create instance id=" + SERVICE_INSTANCE_ID);
        String customSecretsMessage = (String) context.contextKeys.get(SECRETS_REPOSITORY_ALIAS_NAME+GitProcessorContext.commitMessage.toString());
        assertThat(customSecretsMessage).isEqualTo("Cassandra broker: create instance id=" + SERVICE_INSTANCE_ID);
    }

    @Test
    public void responds_to_get_last_service_operation_in_progress() {

        //Given a get last operation request (asynchronous polling from Cloud Controller)
        GetLastServiceOperationRequest operationRequest = new GetLastServiceOperationRequest(SERVICE_INSTANCE_ID,
                "service_definition_id",
                "plan_id",
                CassandraProcessorConstants.OSB_OPERATION_CREATE);

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST, operationRequest);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (in progress state)
        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse();
        expectedResponse.withOperationState(OperationState.IN_PROGRESS);
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        when(tracker.getDeploymentExecStatus(any(Path.class), eq(SERVICE_INSTANCE_ID), eq(CassandraProcessorConstants.OSB_OPERATION_CREATE), any(GetLastServiceOperationRequest.class))).thenReturn(expectedResponse);


        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra");
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
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra");
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
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra");
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
                CassandraProcessorConstants.OSB_OPERATION_CREATE);

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST, operationRequest);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (succeeded state)
        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse();
        expectedResponse.withDescription("Creation is succeeded");
        expectedResponse.withOperationState(OperationState.SUCCEEDED);
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        when(tracker.getDeploymentExecStatus(any(Path.class), eq(SERVICE_INSTANCE_ID), eq(CassandraProcessorConstants.OSB_OPERATION_CREATE), any(GetLastServiceOperationRequest.class))).thenReturn(expectedResponse);

        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra");
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
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, secretsGenerator, tracker, "Cassandra");
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

        // and with a proper commit message
        String customMessage = (String) context.contextKeys.get(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.commitMessage.toString());
        assertThat(customMessage).isEqualTo("Cassandra broker" + ": "+ CassandraProcessorConstants.OSB_OPERATION_DELETE + " instance id=" + SERVICE_INSTANCE_ID);
    }



    private PipelineCompletionTracker aCompletionTracker() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L), ZoneId.of("Europe/Paris"));
        return new PipelineCompletionTracker(clock, 1200L, Mockito.mock(OsbProxy.class));
    }

    private Path aGitRepoWorkDir() {
        return FileSystems.getDefault().getPath("/a/git_workdir/path");
    }

}