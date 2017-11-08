package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.*;
import org.assertj.core.api.Assertions;
import org.fest.assertions.MapAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.servicebroker.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ProcessorChainServiceInstanceServiceTest {

    @Mock
    ProcessorChain processorChain;

    @InjectMocks
    ProcessorChainServiceInstanceService processorChainServiceInstanceService;

    @Test
    public void should_chain_create_processors_on_service_instance_creation() throws Exception {
        //given
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id");

        //when
        CreateServiceInstanceResponse response = processorChainServiceInstanceService.createServiceInstance(request);

        //then
        Assertions.assertThat(response).isEqualTo(new CreateServiceInstanceResponse());
        Mockito.verify(processorChain).create(any(Context.class));

    }

    @Test
    public void should_set_context_key_for_request() {
        //Given
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id");

        //when
        CreateServiceInstanceResponse response = processorChainServiceInstanceService.createServiceInstance(request);

        //then

        ArgumentCaptor<Context> argument = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(processorChain).create(argument.capture());

        Context ctx=argument.getValue();

        assertThat(ctx.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST)).isEqualTo(request);
    }

    @Test
    public void should_use_response_from_context_when_set() {
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id");

        //given a processor that generates a response into the context
        final CreateServiceInstanceResponse customResponse = new CreateServiceInstanceResponse()
                .withDashboardUrl("https://a.dashboard.org");

        BrokerProcessor processor = new DefaultBrokerProcessor() {
            @Override
            public void preCreate(Context ctx) {
                ctx.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE, customResponse);
            }
        };
        processorChain = aProcessorChain(processor);
        processorChainServiceInstanceService = new ProcessorChainServiceInstanceService(processorChain);


        //when
        CreateServiceInstanceResponse response = processorChainServiceInstanceService.createServiceInstance(request);

        //then
        Assertions.assertThat(response).isEqualTo(customResponse);
    }

    @Test
    public void should_chain_getLastCreateOperation_processors() throws Exception {
        //given
        GetLastServiceOperationRequest request = new GetLastServiceOperationRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id");

        //when
        GetLastServiceOperationResponse response = processorChainServiceInstanceService.getLastOperation(request);

        //then
        Assertions.assertThat(response).isEqualTo(new GetLastServiceOperationResponse());
        Mockito.verify(processorChain).getLastCreateOperation(any(Context.class));

    }

    @Test
    public void should_chain_delete_processors_on_service_instance_deletion() throws Exception {
        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest("instance_id",
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                true);

        DeleteServiceInstanceResponse response = processorChainServiceInstanceService.deleteServiceInstance(request);

        Assertions.assertThat(response).isEqualTo(new DeleteServiceInstanceResponse());
        Mockito.verify(processorChain).delete();
    }

    public ProcessorChain aProcessorChain(BrokerProcessor processor) {
        List<BrokerProcessor> processors=new ArrayList<BrokerProcessor>();
        processors.add(new DefaultBrokerProcessor());
        processors.add(processor);
        DefaultBrokerSink sink=new DefaultBrokerSink();
        ProcessorChain chain=new ProcessorChain(processors, sink);
        return chain;
    }


}