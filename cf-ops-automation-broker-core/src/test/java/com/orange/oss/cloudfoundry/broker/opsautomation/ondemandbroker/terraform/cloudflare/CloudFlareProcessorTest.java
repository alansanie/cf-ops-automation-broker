package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.*;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
public class CloudFlareProcessorTest {

    CloudFlareProcessor cloudFlareProcessor = new CloudFlareProcessor(aConfig());


    @Test
    public void receives_service_instance_creation_inputs() {
        //given a user performing
        //cf cs cloudflare -c '{route="arequestedroute"}'
        Context context = aContextWithCreateRequest("route", "arequestedroute");

        //when the processor is invoked
        String requestedRoute = cloudFlareProcessor.getRequestedRoute(context, "route");

        //then
        assertThat(requestedRoute).isEqualTo("arequestedroute");

    }

    @Test
    public void rejects_invalid_requested_routes() {
        //Given an invalid route
        Context context = aContextWithCreateRequest("route", "@");

        try {
            cloudFlareProcessor.preCreate(context);
            Assert.fail("expected to be rejected");
        } catch (RuntimeException e) {
            //Message should indicate to end user the incorrect param name and value
            assertThat(e.getMessage()).contains("route");
            assertThat(e.getMessage()).contains("@");
        }
    }

    @Test
    public void injects_tf_module_into_context() {
        //given a tf module template available in the classpath
        TerraformModule deserialized = TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
        //TODO: inject into config

        //given a user request with a route
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("route", "avalidroute");
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                parameters
        );
        request.withServiceInstanceId("serviceinstance_guid");

        //and the context being injected to a cloudflare processor
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, request);

        //when
        cloudFlareProcessor.preCreate(context);

        //then it injects a terraform module into the context
        TerraformModule terraformModule = (TerraformModule) context.contextKeys.get(TerraformModuleProcessor.ADD_TF_MODULE_WITH_ID);

        ImmutableTerraformModule expectedModule = ImmutableTerraformModule.builder().from(deserialized)
                .moduleName(request.getServiceInstanceId())
                .putProperties("org_guid", "org_id")
                .putProperties("route-prefix", "avalidroute")
                .putProperties("service_instance_guid", "3456")
                .putProperties("space_guid", "space_id")
                .build();

        assertThat(terraformModule).isEqualTo(expectedModule);
    }


    @Test
    public void rejects_duplicate_route_request() {

    }

    Context aContextWithCreateRequest(String key, String value) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(key, value);
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                parameters
        );

        //and the context being injected to a cloudflare processor
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, request);
        return context;
    }

    private CloudFlareConfig aConfig() {
        String routeSuffix = "-cdn-cw-vdr-pprod-apps.elpaaso.net";
        return new CloudFlareConfig(routeSuffix);
    }


}
