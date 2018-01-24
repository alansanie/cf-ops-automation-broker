package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.google.gson.*;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Created by ijly7474 on 22/01/18.
 */
public class PipelineOperationStateGsonAdapter implements JsonDeserializer<PipelineCompletionTracker.PipelineOperationState>, JsonSerializer<PipelineCompletionTracker.PipelineOperationState> {

    //"{\"serviceBrokerRequest\":{\"serviceDefinitionId\":\"service_definition_id\",\"planId\":\"plan_id\",\"organizationGuid\":\"org_id\",\"spaceGuid\":\"space_id\",\"parameters\":{\"paramaterName\":\"paramaterValue\"},\"asyncAccepted\":false},\"startRequestDate\":\"2018-01-22T14:00:00.000Z\"}"


    @Override
    public JsonElement serialize(PipelineCompletionTracker.PipelineOperationState pipelineOperationState, Type type, JsonSerializationContext jsonSerializationContext) {

        final JsonObject jsonObject = new JsonObject();
        ServiceBrokerRequest request = pipelineOperationState.getServiceBrokerRequest();
        String startRequestDate = pipelineOperationState.getStartRequestDate();
        String classFullyQualifiedName = request.getClass().getName();
        JsonElement jsonElementRequest;
        switch(classFullyQualifiedName)
        {
            case CassandraProcessorConstants.OSB_CREATE_REQUEST_CLASS_NAME:
                jsonElementRequest = jsonSerializationContext.serialize(request, CreateServiceInstanceRequest.class);
                jsonObject.add(CassandraProcessorConstants.OSB_CREATE_REQUEST_CLASS_NAME, jsonElementRequest);
                break;
            case CassandraProcessorConstants.OSB_DELETE_REQUEST_CLASS_NAME:
                jsonElementRequest = jsonSerializationContext.serialize(request, DeleteServiceInstanceRequest.class);
                jsonObject.add(CassandraProcessorConstants.OSB_DELETE_REQUEST_CLASS_NAME, jsonElementRequest);
                break;
            default:
                throw new RuntimeException("PipelineOperationStateGsonAdapter serialize method fails");
        }
        JsonElement jsonElementStartRequestDate = jsonSerializationContext.serialize(startRequestDate, String.class);
        jsonObject.add("startRequestDate", jsonElementStartRequestDate);

        return jsonObject;
    }

    @Override
    public PipelineCompletionTracker.PipelineOperationState deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        ServiceBrokerRequest serviceBrokerRequest = null;
        String startRequestDate = null;
        for (Map.Entry<String, JsonElement> entry : entries) {
            String className = entry.getKey();
            switch(className)
            {
                case CassandraProcessorConstants.OSB_CREATE_REQUEST_CLASS_NAME:
                serviceBrokerRequest = jsonDeserializationContext.deserialize(entry.getValue(), CreateServiceInstanceRequest.class);
                break;
                case CassandraProcessorConstants.OSB_DELETE_REQUEST_CLASS_NAME:
                serviceBrokerRequest = jsonDeserializationContext.deserialize(entry.getValue(), DeleteServiceInstanceRequest.class);
                break;
                case "startRequestDate":
                startRequestDate = jsonDeserializationContext.deserialize(entry.getValue(), String.class);
                break;
            }
        }

        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(serviceBrokerRequest, startRequestDate);

        return pipelineOperationState;
    }
}