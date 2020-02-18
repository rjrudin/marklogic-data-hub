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

    let job = endpointState.job;
    if (!job) {
      job = datahub.jobs.newJob(flowName, jobId);
      // Store the job data in the endpointState without the unnecessary "job" root property
      endpointState.job = job.job;
      jobMustBeSaved = true;
    } else {
      // The job document is expected to be persisted with a root "job" property
      job = {job: job};
    }

    let stepResponse = job.job.stepResponses[stepNumber];
    if (!stepResponse) {
      job = datahub.jobs.buildWithStepStarted(job, stepNumber);
      stepResponse = job.job.stepResponses[stepNumber];
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
      job = datahub.jobs.buildWithCompletedStep(job, stepNumber);

      const stepIndex = stepNumbers.indexOf(stepNumber);
      if (stepIndex >= stepNumbers.length - 1) {
        datahub.jobs.completeJob(job);
        // Returning null indicates that there are no items left to be processed by this step
        return null;
      } else {
        datahub.jobs.saveJob(job);
        endpointState.stepNumber = stepNumbers[stepIndex + 1];
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
}


module.exports = StepRunner;
