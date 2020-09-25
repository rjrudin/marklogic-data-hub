package com.marklogic.hub.cloud.aws.services.stepfunctions;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessResult;
import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.hub.HubClient;
import com.marklogic.hub.flow.FlowInputs;
import com.marklogic.hub.flow.FlowRunner;
import com.marklogic.hub.flow.RunFlowResponse;
import com.marklogic.hub.flow.impl.FlowRunnerImpl;
import com.marklogic.hub.impl.HubConfigImpl;

import java.util.concurrent.TimeUnit;

/**
 * Sample program for polling a Step Functions Activity ARN and then running a hardcoded step. 
 */
public class RunStepActivity extends LoggingObject {

    private AWSStepFunctions stepFunctionsClient;
    private String activityArn;
    private HubClient hubClient;

    /**
     * Depends on AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY being set as environment variables or
     * system properties. See DefaultAWSCredentialsProviderChain for more information.
     *
     * @param args
     */
    public static void main(final String[] args) {
        final String activityArn = args[0];
        new RunStepActivity(activityArn).poll();
    }

    public RunStepActivity(String activityArn) {
        this.activityArn = activityArn;

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSocketTimeout((int) TimeUnit.SECONDS.toMillis(70));

        stepFunctionsClient = AWSStepFunctionsClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .withClientConfiguration(clientConfiguration)
            .build();

        // This of course would be passed in via the constructor
        // Just hard-coding for demo purposes
        this.hubClient = new HubConfigImpl("localhost", "admin", "admin").newHubClient();
    }

    public void poll() {
        while (true) {
            GetActivityTaskRequest request = new GetActivityTaskRequest().withActivityArn(activityArn);
            GetActivityTaskResult result = stepFunctionsClient.getActivityTask(request);
            if (result.getTaskToken() != null) {
                runStep(result);
            } else {
                sleep();
            }
        }
    }

    /**
     * Currently depends on the reference-entity-model project being deployed. All of this is purposefully
     * hardcoded for sake of a prototype.
     */
    private void runStep(GetActivityTaskResult getActivityTaskResult) {
        logger.info("Task input: " + getActivityTaskResult.getInput());
        // TODO Will soon actually care about the task input
        //JsonNode json = Jackson.jsonNodeOf(getActivityTaskResult.getInput());

        // Just run this step for now
        FlowInputs inputs = new FlowInputs("CurateCustomerJSON", "2");
        FlowRunner flowRunner = new FlowRunnerImpl(hubClient);
        RunFlowResponse response = flowRunner.runFlow(inputs);
        flowRunner.awaitCompletion();

        SendTaskSuccessResult sendTaskSuccessResult = stepFunctionsClient.sendTaskSuccess(new SendTaskSuccessRequest()
            .withOutput(response.toJson())
            .withTaskToken(getActivityTaskResult.getTaskToken())
        );
        logger.info("Sent success, result: " + sendTaskSuccessResult);
    }

    private void sleep() {
        logger.debug("Sleeping");
        try {
            // TODO This will be user-configurable
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.warn("Ignoring InterruptedException: " + e.getMessage());
        }
    }

}
