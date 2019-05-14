package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.servicebroker.autoconfigure.web.ServiceBrokerAutoConfiguration;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class YamlCataloglAsEnvironmentVarApplicationContextInitializerTest {

    @Before
    public void init() {
        System.setProperty("CATALOG_YML", CATALOG_YML);
        System.setProperty("spring.cloud.openservicebroker.catalog.services[0].plans[0].description", "overriden-by-individual-property");
        assertThat(System.getProperty("CATALOG_YML")).isNotEmpty();
    }

    @After
    public void after() {
        System.clearProperty("CATALOG_YML");
        assertThat(System.getProperty("CATALOG_YML")).isNull();
        System.clearProperty("spring.cloud.openservicebroker.catalog.services[0].plans[0].description");
        assertThat(System.getProperty("spring.cloud.openservicebroker.catalog.services[0].plans[0].description")).isNull();
    }

    private final YamlCataloglAsEnvironmentVarApplicationContextInitializer contextInitializer = new YamlCataloglAsEnvironmentVarApplicationContextInitializer();

    // The CATALOG_YML isn't prefixed by spring.cloud.openservicebroker to preserve legacy key format
    public static final String CATALOG_YML = "servicebroker:\n" +
            "   catalog:\n" +
            "     services:\n" +
            "       - id: ondemand-service\n" +
            "         name: ondemand\n" +
            "         description: try a simple ondemand service broker implementation\n" +
            "         bindable: true\n" +
            "         plans:\n" +
            "           - id: ondemand-plan\n" +
            "             name: default\n" +
            "             description: This is a default ondemand plan.  All services are created equally.\n" +
            "         tags:\n" +
            "           -ondemand\n" +
            "           -document\n" +
            "         metadata:\n" +
            "           displayName: ondemand\n" +
            "           imageUrl: https://orange.com/image.png\n" +
            "           longDescription: ondemand Service\n" +
            "           providerDisplayName: Orange\n" +
            "           documentationUrl: https://orange.com/doc\n" +
            "           supportUrl: https://orange.com/support\n";

    @Test
    public void loads_yml_env_var_as_last_property_source_into_spring_context_and_convert_to_scosb_format() {
        //given a CATALOG_YML env var is defined
        StaticApplicationContext context = new StaticApplicationContext();

        //when
        contextInitializer.initialize(context);

        //then a property source is indeed added
        assertThat(context.getEnvironment().getPropertySources().contains("catalog_from_env_var")).isTrue();

        //and properties within this entries have been converted
        PropertySource<?> catalogFromEnvVar = context.getEnvironment().getPropertySources().get("catalog_from_env_var");
        assertThat(catalogFromEnvVar.getProperty("spring.cloud.openservicebroker.catalog.services[0].id")).isEqualTo("ondemand-service");
        assertThat(catalogFromEnvVar.getProperty("spring.cloud.openservicebroker.catalog.services[0].name")).isEqualTo("ondemand");
        // same with nested properties
        assertThat(catalogFromEnvVar.getProperty("spring.cloud.openservicebroker.catalog.services[0].metadata.imageUrl")).isEqualTo("https://orange.com/image.png");

        assertThat(context.getEnvironment().getProperty("spring.cloud.openservicebroker.catalog.services[0].plans[0].description")).isEqualTo("overriden-by-individual-property");
    }

    @Test
    public void converts_legacy_to_scosb_keys() {
        //given
        Map<String, String> source = new HashMap<>();
        source.put("servicebroker.catalog.services[0].id", "ondemand-service");
        source.put("servicebroker.catalog.services[0].name", "ondemand");
        //when
        contextInitializer.convertPropertySourceToScOsbKeyPrefix(source);
        //then
        assertThat(source).containsOnly(
                entry("spring.cloud.openservicebroker.catalog.services[0].id", "ondemand-service"),
                entry("spring.cloud.openservicebroker.catalog.services[0].name", "ondemand"));
    }

    @Test
    public void does_not_convert_scosb_keys() {
        //given
        Map<String, String> source = new HashMap<>();
        source.put("spring.cloud.openservicebroker.catalog.services[0].id", "ondemand-service");
        source.put("spring.cloud.openservicebroker.catalog.services[0].name", "ondemand");
        //when
        contextInitializer.convertPropertySourceToScOsbKeyPrefix(source);
        //then
        assertThat(source).containsOnly(
                entry("spring.cloud.openservicebroker.catalog.services[0].id", "ondemand-service"),
                entry("spring.cloud.openservicebroker.catalog.services[0].name", "ondemand"));
    }


    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServiceBrokerAutoConfiguration.class))
            .withInitializer(new YamlCataloglAsEnvironmentVarApplicationContextInitializer());


    /**
     * Declares a dummy service instance service, which is necessary for the osb lib autoconfiguration to trigger
     * and fetch the catalog
     */
    @Configuration
    public static class NoCatalogBeanConfiguration {
        @Bean
        public ServiceInstanceService serviceInstanceService() {
            return new TestServiceInstanceService();
        }

        public static class TestServiceInstanceService implements ServiceInstanceService {
            @Override
            public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
                return null;
            }

            @Override
            public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
                return null;
            }

            @Override
            public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
                return null;
            }

            @Override
            public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
                return null;
            }
        }
    }


    /**
     * Inspired from org.springframework.cloud.servicebroker.autoconfigure.web.ServiceBrokerAutoConfigurationTest#servicesAreCreatedFromCatalogProperties
     * See https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/8cad269c90393857e2ebc36223472ec68a5e2401/spring-cloud-open-service-broker-autoconfigure/src/test/java/org/springframework/cloud/servicebroker/autoconfigure/web/ServiceBrokerAutoConfigurationTest.java#L89
     */
    @Test
    public void loads_yml_env_vars_as_catalog_bean() throws Exception {
        this.contextRunner
                .withUserConfiguration(NoCatalogBeanConfiguration.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(Catalog.class);
                    Catalog catalog = context.getBean(Catalog.class);
                    assertThat(catalog.getServiceDefinitions()).hasSize(1);
                    assertThat(catalog.getServiceDefinitions().get(0).getId()).isEqualTo("ondemand-service");
                    assertThat(catalog.getServiceDefinitions().get(0).getName()).isEqualTo("ondemand");
                    assertThat(catalog.getServiceDefinitions().get(0).getDescription()).isEqualTo("try a simple ondemand service broker implementation");
                    assertThat(catalog.getServiceDefinitions().get(0).getPlans()).hasSize(1);
                    assertThat(catalog.getServiceDefinitions().get(0).getPlans().get(0).getId()).isEqualTo("ondemand-plan");
                    assertThat(catalog.getServiceDefinitions().get(0).getPlans().get(0).getName()).isEqualTo("default");
                    assertThat(catalog.getServiceDefinitions().get(0).getPlans().get(0).getDescription()).isEqualTo("This is a default ondemand plan.  All services are created equally.");
                });
    }
}

