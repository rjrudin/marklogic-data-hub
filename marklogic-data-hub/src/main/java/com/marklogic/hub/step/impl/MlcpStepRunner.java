package com.marklogic.hub.step.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.contentpump.ContentPump;
import com.marklogic.contentpump.bean.MlcpBean;
import com.marklogic.hub.HubClient;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.job.JobStatus;
import com.marklogic.hub.step.RunStepResponse;
import com.marklogic.hub.step.StepItemCompleteListener;
import com.marklogic.hub.step.StepItemFailureListener;
import com.marklogic.hub.step.StepRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MlcpStepRunner extends LoggingObject implements StepRunner {

    private HubClient hubClient;

    private Flow flow;
    private String stepNumber;
    private String jobId;
    private Integer batchSize;
    private Integer threadCount;
    private Map<String, Object> options;
    private Map<String, Object> stepConfig;

    public MlcpStepRunner(HubClient hubClient) {
        this.hubClient = hubClient;
    }

    @Override
    public RunStepResponse run() {
        String transformParam = "flow-name=" + this.flow.getName() +
            ",step=" + this.stepNumber + ",options={\"permissions\":\"data-hub-operator,read,data-hub-developer,update\",\"provenanceGranularityLevel\":\"off\"}";

        MlcpBean mlcpBean = new MlcpBean();

        mlcpBean.setCommand("IMPORT");
        mlcpBean.setHost(hubClient.getStagingClient().getHost());
        mlcpBean.setUsername(hubClient.getUsername());
        mlcpBean.setPassword("password"); // TODO

        // Assume staging database, will change if target database is set
        mlcpBean.setPort(8010);
        mlcpBean.setDatabase("data-hub-STAGING");
//        if (hubConfig.getStagingSslContext() != null) {
//            mlcpBean.setSsl(true);
//        }

        if (this.batchSize != null) {
            mlcpBean.setBatch_size(this.batchSize);
        }
        if (this.threadCount != null) {
            mlcpBean.setThread_count(this.threadCount);
        }

        Map<String, String> fileLocations = new HashMap<>();
        if(this.flow.getStep(this.stepNumber).getFileLocations() != null) {
            fileLocations.putAll(new ObjectMapper().convertValue(flow.getStep(stepNumber).getFileLocations(), Map.class));
        }

        if (stepConfig != null) {
            if (stepConfig.get("fileLocations") != null) {
                fileLocations.putAll((Map<String, String>) stepConfig.get("fileLocations"));
            }
            if (stepConfig.containsKey("batchSize")) {
                mlcpBean.setBatch_size(Integer.parseInt((String) stepConfig.get("batchSize")));
            }
            if (stepConfig.containsKey("threadCount")) {
                mlcpBean.setThread_count(Integer.parseInt((String) stepConfig.get("threadCount")));
            }
        }

        mlcpBean.setInput_file_path(fileLocations.get("inputFilePath"));

        mlcpBean.setInput_file_type("documents");
        //mlcpBean.setInput_file_type(fileLocations.get("inputFileType"));


        mlcpBean.setOutput_uri_replace(fileLocations.get("outputURIReplacement"));
        mlcpBean.setOutput_uri_prefix(fileLocations.get("outputURIPrefix"));
        // TODO separator

        if (options != null) {
            if (options.containsKey("collections")) {
                mlcpBean.setOutput_collections((String) options.get("collections"));
            }
            if (options.containsKey("permissions")) {
                mlcpBean.setOutput_permissions((String) options.get("permissions"));
            }
            if (options.containsKey("outputFormat")) {
                mlcpBean.setDocument_type((String) options.get("outputFormat"));
            }

            if (options.containsKey("targetDatabase")) {
                // TODO Set port, do SSL check
                mlcpBean.setDatabase((String) options.get("targetDatabase"));
            }
        }

        mlcpBean.setOutput_collections("aaa");
        mlcpBean.setOutput_permissions("data-hub-common,read,data-hub-common,update");
        //mlcpBean.setTransform_module("/data-hub/5/transforms/mlcp-flow-transform.sjs");
        //mlcpBean.setModules_root("/");
        //mlcpBean.setTransform_param(transformParam);

//        if (hubConfig.getIsHostLoadBalancer()) {
//            mlcpBean.setRestrict_hosts(true);
//        }

        RunStepResponse response = RunStepResponse.withFlow(flow)
            .withStep(stepNumber)
            .withStatus(JobStatus.COMPLETED_PREFIX + stepNumber)
            .withJobId(jobId);
        response.setTargetDatabase(mlcpBean.getDatabase());
        // TODO Parse this from MLCP output
        //response.setCounts()

        try {
            ContentPump.runCommand(mlcpBean.buildArgs());
        } catch (IOException e) {
            throw new RuntimeException("Oops: " + e.getMessage(), e);
        }

        return response;
    }

    @Override
    public StepRunner withFlow(Flow flow) {
        this.flow = flow;
        return this;
    }

    @Override
    public StepRunner withBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    @Override
    public StepRunner withStep(String step) {
        this.stepNumber = step;
        return this;
    }

    @Override
    public StepRunner withJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    @Override
    public StepRunner withThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    @Override
    public StepRunner withRuntimeOptions(Map<String, Object> options) {
        this.options = options;
        return this;
    }

    @Override
    public StepRunner withStepConfig(Map<String, Object> stepConfig) {
        // TODO Check for e.g. inputFilePath
        this.stepConfig = stepConfig;
        return this;
    }

    @Override
    public StepRunner withStopOnFailure(boolean stopOnFailure) {
        // Ignoring
        return this;
    }

    @Override
    public StepRunner onItemComplete(StepItemCompleteListener listener) {
        // Ignoring
        return this;
    }

    @Override
    public StepRunner onItemFailed(StepItemFailureListener listener) {
        // Ignoring
        return this;
    }

    @Override
    public void awaitCompletion() {
        // Ignore; run() will be synchronous
    }

    @Override
    public void awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        // Ignore; run() will be synchronous
    }

    @Override
    public RunStepResponse run(Collection<String> uris) {
        throw new UnsupportedOperationException("Running against a set of URIs is not supported");
    }

    @Override
    public void stop() {
        // Ignore
    }

    @Override
    public int getBatchSize() {
        return this.batchSize;
    }
}
