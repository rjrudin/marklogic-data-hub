const hubTest = require("/test/data-hub-test-helper.xqy");
const StepRunner = require("/data-hub/5/impl/step-runner.sjs");
const test = require("/test/test-helper.xqy");

const stepRunner = new StepRunner();

const workUnit = {
  flowName: "PersonFlow",
  stepNumbers: ["1", "2"],
  batchSize: 2,
  jobId: "job123"
};

let endpointState = {};

// Process first batch for first step
let results = stepRunner.runSteps(workUnit, endpointState).toArray();
endpointState = results[0];
let batchResponse = results[1];
let job = endpointState.job;
let stepResponse = job.stepResponses["1"];

let assertions = [
  test.assertEqual("/content/person2.json", endpointState.lastProcessedItem),
  test.assertEqual("job123", endpointState.jobId),

  test.assertEqual(2, stepResponse.totalEvents),
  test.assertEqual(0, stepResponse.failedEvents),
  test.assertEqual(1, stepResponse.successfulBatches),
  test.assertEqual(0, stepResponse.failedBatches),
  test.assertTrue(stepResponse.stepStartTime != null),

  test.assertEqual("job123", batchResponse.jobId),
  test.assertEqual(2, batchResponse.totalCount),
  test.assertEqual(0, batchResponse.errorCount),
  test.assertEqual("/content/person1.json", batchResponse.completedItems[0]),
  test.assertEqual("/content/person2.json", batchResponse.completedItems[1]),

  test.assertEqual("job123", job.jobId),
  test.assertEqual("PersonFlow", job.flow),
  test.assertEqual(xdmp.getCurrentUser(), job.user),
  test.assertEqual("1", job.lastAttemptedStep),
  test.assertTrue(job.timeStarted != null),
  test.assertEqual("running step 1", stepResponse.status),
  test.assertTrue(stepResponse.stepStartTime != null)
];

// Process second batch for first step
results = stepRunner.runSteps(workUnit, endpointState).toArray();
endpointState = results[0];
batchResponse = results[1];

assertions.push(
  test.assertEqual("/content/person3.json", endpointState.lastProcessedItem),
  test.assertEqual("job123", endpointState.jobId),

  test.assertEqual(3, stepResponse.totalEvents),
  test.assertEqual(0, stepResponse.failedEvents),
  test.assertEqual(2, stepResponse.successfulBatches),
  test.assertEqual(0, stepResponse.failedBatches),
  test.assertTrue(stepResponse.stepStartTime != null),

  test.assertEqual("job123", batchResponse.jobId),
  test.assertEqual(1, batchResponse.totalCount),
  test.assertEqual(0, batchResponse.errorCount),
  test.assertEqual("/content/person3.json", batchResponse.completedItems[0])
);

// Process empty batch for first step
results = stepRunner.runSteps(workUnit, endpointState).toArray();
endpointState = results[0];
job = endpointState.job;
stepResponse = job.stepResponses["1"];

// TODO Add assertions on first step being complete!
assertions.push(
  test.assertTrue(endpointState.lastProcessedItem == undefined)
);


// Now process each batch for the second step
results = stepRunner.runSteps(workUnit, endpointState).toArray();
endpointState = results[0];
results = stepRunner.runSteps(workUnit, endpointState).toArray();
endpointState = results[0];
results = stepRunner.runSteps(workUnit, endpointState);

// The second step and job should be finished now, so verify the job document
job = hubTest.getFirstJobDocument().toObject().job;
stepResponse = job.stepResponses["2"];

assertions.concat(
  test.assertEqual(undefined, results,
    "Since all the items have been processed, the endpoint should not return anything, indicating that the step is complete"),

  test.assertEqual("job123", job.jobId),
  test.assertEqual("PersonFlow", job.flow),
  test.assertEqual(xdmp.getCurrentUser(), job.user),
  test.assertEqual("finished", job.jobStatus),
  test.assertTrue(job.timeStarted != null),
  test.assertTrue(job.timeEnded != null),
  test.assertEqual("2", job.lastAttemptedStep),
  test.assertEqual("2", job.lastCompletedStep),

  test.assertEqual(3, stepResponse.totalEvents),
  test.assertEqual(0, stepResponse.failedEvents),
  test.assertEqual(2, stepResponse.successfulBatches),
  test.assertEqual(0, stepResponse.failedBatches),
  test.assertTrue(stepResponse.stepStartTime != null),
  test.assertEqual("completed step 2", stepResponse.status),
  test.assertTrue(stepResponse.stepEndTime != null,
    "Note that stepEndTime will be the same as stepStartTime as all of this is running in the same transaction")
);
