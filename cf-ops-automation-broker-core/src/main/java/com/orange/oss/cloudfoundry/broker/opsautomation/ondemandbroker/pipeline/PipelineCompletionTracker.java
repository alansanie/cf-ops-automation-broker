package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


/**
 * Created by ijly7474 on 04/01/18.
 */
public class PipelineCompletionTracker {

    private static Logger logger = LoggerFactory.getLogger(PipelineCompletionTracker.class.getName());

    protected Clock clock;
    private Gson gson;
    private long maxExecutionDurationSeconds = 600L;

    public PipelineCompletionTracker(Clock clock) {
        this.clock = clock;
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PipelineCompletionTracker.PipelineOperationState.class, new PipelineOperationStateGsonAdapter());
        this.gson = gsonBuilder.create();
    }

    public GetLastServiceOperationResponse getDeploymentExecStatus(Path secretsWorkDir, String serviceInstanceId, String jsonPipelineOperationState) {

        //Check if target manifest file is present
        Path targetManifestFile = this.getTargetManifestFilePath(secretsWorkDir, serviceInstanceId);
        boolean isTargetManifestFilePresent = Files.exists(targetManifestFile);

        //Initialize response and determine the appropriate values based on pipelineOperationState
        GetLastServiceOperationResponse response = new GetLastServiceOperationResponse();
        PipelineOperationState pipelineOperationState = this.parseFromJson(jsonPipelineOperationState);
        long elapsedTimeSecsSinceStartRequestDate = this.getElapsedTimeSecsSinceStartRequestDate(pipelineOperationState.getStartRequestDate());
        boolean isRequestTimedOut = this.isRequestTimedOut(elapsedTimeSecsSinceStartRequestDate);
        if (isRequestTimedOut){
            response.withOperationState(OperationState.FAILED);
            response.withDescription("execution timeout after " + elapsedTimeSecsSinceStartRequestDate + "s max is " + maxExecutionDurationSeconds);
        }else{
            ServiceBrokerRequest serviceBrokerRequest = pipelineOperationState.getServiceBrokerRequest();
            String classFullyQualifiedName = serviceBrokerRequest.getClass().getName();
            switch(classFullyQualifiedName)
            {
                case CassandraProcessorConstants.OSB_CREATE_REQUEST_CLASS_NAME:
                    if (isTargetManifestFilePresent){
                        response.withOperationState(OperationState.SUCCEEDED);
                        response.withDescription("Creation is succeeded");
                    }else{
                        response.withOperationState(OperationState.IN_PROGRESS);
                        response.withDescription("Creation is in progress");
                    }
                    break;
                default:
                    throw new RuntimeException("Get Deployment Execution status fails (unhandled request class)");
            }
        }
        return response;
    }

    public Path getTargetManifestFilePath(Path workDir, String serviceInstanceId) {
        return StructureGeneratorHelper.generatePath(workDir,
                    CassandraProcessorConstants.ROOT_DEPLOYMENT_DIRECTORY,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId,
                    CassandraProcessorConstants.SERVICE_INSTANCE_PREFIX_DIRECTORY + serviceInstanceId + CassandraProcessorConstants.YML_SUFFIX);
    }

    private String getCurrentDate() {
        return Instant.now(clock).toString();
    }

    private boolean isRequestTimedOut(long elapsedTimeSecsSinceStartRequestDate){
        return elapsedTimeSecsSinceStartRequestDate >= this.maxExecutionDurationSeconds;
    }

    long getElapsedTimeSecsSinceStartRequestDate(String startRequestDate) {
        Instant start = Instant.parse(startRequestDate);
        Instant now = Instant.now(clock);
        long elapsedSeconds = start.until(now, ChronoUnit.SECONDS);
        if (elapsedSeconds < 0) {
            logger.error("Unexpected start request date in future:" + startRequestDate + " Is there a clock desynchronized around ?");
            //We don't know who's wrong so, so we don't trigger a service instance failure.
        }
        return elapsedSeconds;
    }

    public String getPipelineOperationStateAsJson(ServiceBrokerRequest serviceBrokerRequest) {
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(
                serviceBrokerRequest,
                getCurrentDate()

        );
        return formatAsJson(pipelineOperationState);
    }

    String formatAsJson(PipelineCompletionTracker.PipelineOperationState pipelineOperationState) {
        return gson.toJson(pipelineOperationState);
    }

    PipelineCompletionTracker.PipelineOperationState parseFromJson(String json) {
        return gson.fromJson(json, PipelineCompletionTracker.PipelineOperationState.class);
    }

    static class PipelineOperationState {
        private ServiceBrokerRequest serviceBrokerRequest;
        private String startRequestDate;

        public PipelineOperationState(ServiceBrokerRequest serviceBrokerRequest, String startRequestDate) {
            this.serviceBrokerRequest = serviceBrokerRequest;
            this.startRequestDate = startRequestDate;
        }

        public ServiceBrokerRequest getServiceBrokerRequest(){
            return this.serviceBrokerRequest;
        }

        public String getStartRequestDate(){
            return this.startRequestDate;
        }

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PipelineOperationState that = (PipelineOperationState) o;

            if (!startRequestDate.equals(that.startRequestDate)) return false;
            return serviceBrokerRequest.equals(that.serviceBrokerRequest);
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        @Override
        public int hashCode() {
            int result = 31 * startRequestDate.hashCode();
            return result;
        }
    }



}
