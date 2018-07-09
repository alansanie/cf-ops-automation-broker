package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is typically imported by SpringBoot apps or Tests by referencing it into
 * EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "deployment")
public class DeploymentProperties {

    private String brokerDisplayName;  // e.g. "Cassandra",  used in broker traces.
    private String rootDeployment;  //e g.. "coab-depls"; //Root deployment (i.e coab-depls)
    private String modelDeployment ; // e.g. "cassandravarsops", Model deployment (i.e cassandra, cassandravarsops, ...)
    private String modelDeploymentShortAlias ; // e.g. "c", shortname for the model deployment. Enables distinguishing services. Should be short so that broker URL remains shorter than 63 chars

    public String getRootDeployment() {
        return rootDeployment;
    }

    public void setRootDeployment(String rootDeployment) {
        this.rootDeployment = rootDeployment;
    }

    public String getModelDeployment() {
        return modelDeployment;
    }

    public void setModelDeployment(String modelDeployment) {
        this.modelDeployment = modelDeployment;
    }

    public String getModelDeploymentShortAlias() {
        return modelDeploymentShortAlias;
    }

    public void setModelDeploymentShortAlias(String modelDeploymentShortAlias) {
        this.modelDeploymentShortAlias = modelDeploymentShortAlias;
    }

    public String getBrokerDisplayName() {
        return brokerDisplayName;
    }

    public void setBrokerDisplayName(String brokerDisplayName) {
        this.brokerDisplayName = brokerDisplayName;
    }
}

