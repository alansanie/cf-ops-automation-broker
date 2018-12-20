package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbConstants.ORIGINATING_EMAIL_KEY;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbConstants.ORIGINATING_USER_KEY;
import static java.util.Arrays.asList;

public class OsbBuilderHelper {



    @SuppressWarnings("WeakerAccess")
    public static DeleteServiceInstanceBindingRequest anUnbindRequest(String serviceInstanceId, String bindingId) {
        ServiceDefinition serviceDefinition = aCatalog().getServiceDefinitions().get(0);
        DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
            .serviceDefinition(serviceDefinition)
                .bindingId(bindingId)
                .serviceDefinitionId("coab-serviceid")
                .planId("coab-planid")
                .serviceDefinition(serviceDefinition)
                .apiInfoLocation("api-info")
                .originatingIdentity(aCfUserContext())
                .platformInstanceId("cf-instance-id").build();
        return request;
    }

    public static Catalog aCatalog() {
        Plan plan  = Plan.builder().id("plan_id").name("plan_name").description("plan_description").metadata(new HashMap<>()).build();
        Plan plan2 = Plan.builder().id("plan_id2").name("plan_name2").description("plan_description2").metadata(new HashMap<>()).build();
        Plan plan3 = Plan.builder().id("plan_id3").name("plan_name3").description("plan_description3").metadata(new HashMap<>()).build();
        ServiceDefinition serviceDefinition =  ServiceDefinition.builder()
                .id("service_id")
                .name("service_name")
                .description("service_description")
                .bindable(true)
                .plans(asList(plan, plan2)).build();

        ServiceDefinition serviceDefinition2 = ServiceDefinition.builder()
                .id("service_id2")
                .name("service_name2")
                .description("service_description3")
                .bindable(true)
                .plans(Collections.singletonList(plan3)).build();
        return Catalog.builder().serviceDefinitions(serviceDefinition
//                , serviceDefinition2 //not yet used
        ).build();
    }

    public static CreateServiceInstanceBindingRequest aBindingRequest(String serviceInstanceId) {
        Map<String, Object> routeBindingParams= new HashMap<>();
        Map<String, Object> serviceBindingParams= new HashMap<>();
        serviceBindingParams.put("user-name", "myname");
        BindResource bindResource = BindResource.builder()
                .appGuid("app_guid")
                .route(null)
                .properties(routeBindingParams)
                .build();

        Context cfContext = aCfContext();

        CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
                .serviceDefinitionId("coab-serviceid")
                .planId("coab-planid")
                .bindResource(bindResource)
                .context(cfContext)
                .parameters(serviceBindingParams)
                .bindingId("service-instance-binding-id")
                .serviceInstanceId(serviceInstanceId)
                .apiInfoLocation("api-info")
                .originatingIdentity(aCfUserContext())
                .platformInstanceId("cf-instance-id")
                .build();
        return request;
    }

    public static Context aCfContext() {
        return CloudFoundryContext.builder()
                .organizationGuid("org_guid")
                .spaceGuid("space_guid")
                .build();
    }

    static CreateServiceInstanceAppBindingResponse aBindingResponse() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("keyspaceName", "ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("contact-points", "127.0.0.1");
        credentials.put("password", "aPassword");
        credentials.put("port", "9142");
        credentials.put("jdbcUrl", "jdbc:cassandra://127.0.0.1:9142/ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("login", "rbbbbbbbb_ba66_4841_018d_2e4d4ce91f47");
        return CreateServiceInstanceAppBindingResponse.builder()
                .credentials(credentials)
                .bindingExisted(false)
                .build();
    }

    public static Context aCfUserContext() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ORIGINATING_USER_KEY, "user_guid");
        properties.put(ORIGINATING_EMAIL_KEY, "user_email");

        return CloudFoundryContext.builder()
                .properties(properties).build();
    }

    @SuppressWarnings("WeakerAccess")
    public static CreateServiceInstanceRequest aCreateServiceInstanceRequest(){
        //Given a parameter request
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameterName", "parameterValue");

        return CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_definition_id")
                .planId("plan_id")
                .parameters(parameters)
                .serviceInstanceId("service-instance-guid")
                .context(CloudFoundryContext.builder()
                    .organizationGuid("org_id")
                    .spaceGuid("space_id")
                    .build()
                )
                .build();
    }

    public static DeleteServiceInstanceRequest aDeleteServiceInstanceRequest() {
        // Given an incoming delete request
        return DeleteServiceInstanceRequest.builder()
                .serviceInstanceId("instance_id")
                .serviceDefinitionId("service_id")
                .planId("plan_id")
                .serviceDefinition(ServiceDefinition.builder().build())
                .asyncAccepted(true)
                .build();
    }

    public static UpdateServiceInstanceRequest anUpdateServiceInstanceRequest() {
        // Given an incoming delete request
        return UpdateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_id")
                .planId("plan_id")
                .parameters(new HashMap<>())
                .serviceInstanceId("instance_id")
                .build();
    }
}
