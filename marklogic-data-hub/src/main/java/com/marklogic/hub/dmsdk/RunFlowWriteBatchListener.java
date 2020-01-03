package com.marklogic.hub.dmsdk;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.datamovement.WriteBatch;
import com.marklogic.client.datamovement.WriteBatchListener;
import com.marklogic.client.datamovement.WriteEvent;
import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.hub.flow.FlowInputs;
import com.marklogic.hub.flow.FlowRunner;
import com.marklogic.hub.flow.RunFlowResponse;
import com.marklogic.hub.flow.impl.FlowRunnerImpl;
import com.marklogic.hub.impl.SimpleHubConfig;
import com.marklogic.hub.step.MarkLogicStepDefinitionProvider;
import com.marklogic.hub.step.StepRunnerFactory;

import java.util.ArrayList;
import java.util.List;

public class RunFlowWriteBatchListener extends LoggingObject implements WriteBatchListener {

    private SimpleHubConfig hubConfig;
    private StepRunnerFactory stepRunnerFactory;
    private FlowInputs flowInputs;

    private boolean awaitCompletion = true;
    private boolean logResponse = true;

    /**
     * Use this constructor when one or more step collectors need to be run, as collectors are not run using a
     * DatabaseClient and thus require a username/password to be specified.
     *
     * @param username
     * @param password
     */
    public RunFlowWriteBatchListener(String host, String username, String password, FlowInputs flowInputs) {
        this.flowInputs = flowInputs;

        hubConfig = new SimpleHubConfig();
        AppConfig appConfig = new AppConfig();
        appConfig.setHost(host);
        hubConfig.setAppConfig(appConfig);
        hubConfig.setHost(host);
        hubConfig.setMlUsername(username);
        hubConfig.setMlPassword(password);

        stepRunnerFactory = new StepRunnerFactory(hubConfig);
        stepRunnerFactory.setStepDefinitionProvider(new MarkLogicStepDefinitionProvider(hubConfig.newStagingClient()));
    }

    /**
     * Use this constructor when no step collectors need to be run, and an existing DatabaseClient can thus be reused.
     *
     * @param stagingClient
     * @param flowInputs
     */
    public RunFlowWriteBatchListener(DatabaseClient stagingClient, FlowInputs flowInputs) {
        this.flowInputs = flowInputs;

        hubConfig = new SimpleHubConfig();
        hubConfig.setStagingClient(stagingClient);
        hubConfig.setHost(stagingClient.getHost());
        AppConfig appConfig = new AppConfig();
        appConfig.setHost(stagingClient.getHost());
        hubConfig.setAppConfig(appConfig);

        stepRunnerFactory = new StepRunnerFactory(hubConfig);
        stepRunnerFactory.setStepDefinitionProvider(new MarkLogicStepDefinitionProvider(stagingClient));
    }

    public RunFlowWriteBatchListener(SimpleHubConfig hubConfig, FlowInputs flowInputs) {
        this.hubConfig = hubConfig;
        this.flowInputs = flowInputs;
    }

    @Override
    public void processEvent(WriteBatch batch) {
        // TODO Creating one of these for each batch as it's not yet known if FlowRunnerImpl is thread-safe
        FlowRunner flowRunner = new FlowRunnerImpl(hubConfig, stepRunnerFactory);
        FlowInputs inputs = newFlowInputs(batch);
        RunFlowResponse response = flowRunner.runFlow(inputs);
        if (awaitCompletion) {
            flowRunner.awaitCompletion();
        }
        if (logResponse) {
            logger.info("Flow response for batch number " + batch.getJobBatchNumber() + ": " + response);
        }
    }

    protected FlowInputs newFlowInputs(WriteBatch batch) {
        FlowInputs newInputs = new FlowInputs(this.flowInputs);
        List<String> uris = new ArrayList<>();
        for (WriteEvent item : batch.getItems()) {
            uris.add(item.getTargetUri());
        }
        newInputs.setUris(uris);
        return newInputs;
    }

    public SimpleHubConfig getHubConfig() {
        return hubConfig;
    }

    public void setAwaitCompletion(boolean awaitCompletion) {
        this.awaitCompletion = awaitCompletion;
    }

    public void setLogResponse(boolean logResponse) {
        this.logResponse = logResponse;
    }
}
