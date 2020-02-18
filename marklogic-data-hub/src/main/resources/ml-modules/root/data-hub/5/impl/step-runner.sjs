'use strict';

const DataHubSingleton = require("/data-hub/5/datahub-singleton.sjs");

const TRACE_EVENT = "datahub-stepRunner";

class StepRunner {

  runSteps(workUnit, endpointState) {
    if (endpointState == null || endpointState == undefined) {
      endpointState = {};
    }
    xdmp.trace(TRACE_EVENT, Sequence.from(["Running step with inputs", workUnit, endpointState]));

    const flowName = workUnit.flowName;
    if (!workUnit.stepNumbers) {
      throw Error("The work unit does not define 'stepNumbers', and thus no step can be run; work unit: " + xdmp.toJsonString(workUnit));
    }
    const stepNumbers = workUnit.stepNumbers;
    const options = workUnit.options || {};

    const datahub = DataHubSingleton.instance({
      performanceMetrics: !!options.performanceMetrics
    });

    const stepNumber = endpointState.stepNumber || stepNumbers[0];

    const jobId = this.determineJobId(workUnit, endpointState);
    let jobMustBeSaved = false;

    let job = datahub.jobs.getJobDocWithId(jobId);
    if (!job) {
      job = datahub.jobs.newJob(flowName, jobId);
      jobMustBeSaved = true;
    }

    let stepResponse = endpointState.stepResponse;
    if (!endpointState.stepResponse) {
      job = datahub.jobs.buildWithStepStarted(job, stepNumber);
      stepResponse = this.buildNewStepResponse();
      endpointState.stepResponse = stepResponse;
      jobMustBeSaved = true;
    }

    // Determine the query and how many results to limit the search to
    const filterQuery = endpointState.lastProcessedItem ?
      cts.rangeQuery(cts.uriReference(), ">", endpointState.lastProcessedItem) :
      null;
    options.contentDescriptorLimit = workUnit.batchSize ? workUnit.batchSize : 100;

    const content = datahub.flow.findMatchingContent(flowName, stepNumber, options, filterQuery);

    if (content == null || content.length < 1) {
      xdmp.trace(TRACE_EVENT, `No matching content found while running step ${stepNumber} in flow ${flowName}`);
      job = datahub.jobs.buildWithCompletedStep(job, stepNumber, endpointState.stepResponse);

      const stepIndex = stepNumbers.indexOf(stepNumber);
      if (stepIndex >= stepNumbers.length - 1) {
        // TODO Check for errors
        datahub.jobs.completeJob(job, "finished");
        // Returning null indicates that there are no items left to be processed by this step
        return null;
      } else {
        datahub.jobs.saveJob(job);
        endpointState.stepNumber = stepNumbers[stepIndex + 1];
        delete endpointState.stepResponse;
        delete endpointState.lastProcessedItem;
        return Sequence.from([endpointState]);
      }
    } else {
      xdmp.trace(TRACE_EVENT, `Found ${content.length} matching items while running step ${stepNumber} in flow ${flowName}`);
      endpointState.lastProcessedItem = content[content.length - 1].uri;
      const batchResponse = datahub.flow.runFlow(flowName, jobId, content, options, stepNumber);
      endpointState.jobId = batchResponse.jobId;

      stepResponse.totalEvents += batchResponse.totalCount;
      stepResponse.failedEvents += batchResponse.errorCount;
      batchResponse.totalCount > batchResponse.errorCount ? stepResponse.successfulBatches++ : stepResponse.failedBatches++;

      if (jobMustBeSaved) {
        datahub.jobs.saveJob(job);
      }

      return Sequence.from([endpointState, batchResponse]);
    }
  }

  determineJobId(workUnit, endpointState) {
    let jobId = workUnit.jobId ? workUnit.jobId : endpointState.jobId;
    return jobId ? jobId : datahub.hubUtils.uuid();
  }

  buildNewStepResponse() {
    return {
      totalEvents: 0,
      failedEvents: 0,
      successfulBatches: 0,
      failedBatches: 0,
      stepStartTime: fn.currentDateTime()
    };
  }
}


module.exports = StepRunner;
