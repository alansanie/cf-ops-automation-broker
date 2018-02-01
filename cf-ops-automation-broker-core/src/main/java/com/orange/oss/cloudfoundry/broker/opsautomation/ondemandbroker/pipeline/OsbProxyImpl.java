package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.CatalogServiceClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient.OsbClientFactory;
import org.springframework.cloud.servicebroker.model.*;

import java.text.MessageFormat;

public class OsbProxyImpl<Q extends ServiceBrokerRequest, P extends AsyncServiceInstanceResponse> implements OsbProxy<Q> {
    private final String osbDelegateUser;
    private final String osbDelegatePassword;
    private String brokerUrlPattern;
    private OsbClientFactory clientFactory;

    public OsbProxyImpl(String osbDelegateUser, String osbDelegatePassword, String brokerUrlPattern, OsbClientFactory clientFactory) {
        this.osbDelegateUser = osbDelegateUser;
        this.osbDelegatePassword = osbDelegatePassword;
        this.brokerUrlPattern = brokerUrlPattern;
        this.clientFactory = clientFactory;
    }

    @Override
    public GetLastServiceOperationResponse delegate(GetLastServiceOperationRequest pollingRequest, CreateServiceInstanceRequest request, GetLastServiceOperationResponse response) {
        return response;
    }

    String getBrokerUrl(CreateServiceInstanceRequest request) {
        String serviceInstanceId = request.getServiceInstanceId();
        this.brokerUrlPattern = "https://{0}-cassandra-broker.mydomain/com";
        String brokerUrlPattern = this.brokerUrlPattern;
        return MessageFormat.format(brokerUrlPattern, serviceInstanceId);
    }

    CatalogServiceClient constructCatalogClient(@SuppressWarnings("SameParameterValue") String brokerUrl) {
        return clientFactory.getClient(brokerUrl, osbDelegateUser, osbDelegatePassword, CatalogServiceClient.class);
    }
}
