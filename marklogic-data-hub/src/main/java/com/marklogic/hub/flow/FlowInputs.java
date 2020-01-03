package com.marklogic.hub.flow;

import java.util.*;

public class FlowInputs {

    private String flowName;
    private List<String> steps;
    private String jobId;
    private Map<String, Object> options;
    private Map<String, Object> stepConfig;
    private Collection<String> uris;

    public FlowInputs() {
    }

    public FlowInputs(String flowName) {
        this.flowName = flowName;
    }

    public FlowInputs(String flowName, String... steps) {
        this.flowName = flowName;
        this.steps = Arrays.asList(steps);
    }

    public FlowInputs(FlowInputs other) {
        this.flowName = other.flowName;
        if (other.steps != null) {
            this.steps = new ArrayList<>();
            this.steps.addAll(other.steps);
        }
        this.jobId = other.jobId;
        if (other.options != null) {
            this.options = new HashMap<>();
            this.options.putAll(other.options);
        }
        if (other.stepConfig != null) {
            this.stepConfig = new HashMap<>();
            this.stepConfig.putAll(other.stepConfig);
        }
        if (other.uris != null) {
            this.uris = new ArrayList<>();
            this.uris.addAll(other.uris);
        }
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public Map<String, Object> getStepConfig() {
        return stepConfig;
    }

    public void setStepConfig(Map<String, Object> stepConfig) {
        this.stepConfig = stepConfig;
    }

    public Collection<String> getUris() {
        return uris;
    }

    public void setUris(Collection<String> uris) {
        this.uris = uris;
    }
}
