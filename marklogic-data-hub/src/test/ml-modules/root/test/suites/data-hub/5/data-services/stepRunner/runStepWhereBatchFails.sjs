const hubTest = require("/test/data-hub-test-helper.xqy");
const StepRunner = require("/data-hub/5/impl/step-runner.sjs");
const test = require("/test/test-helper.xqy");

const stepRunner = new StepRunner();

const workUnit = {
  flowName: "PersonFlow",
  stepNumbers: ["1"],
  batchSize: 2,
  jobId: "job123",
  options: {
    failForUris: [
      "/content/person1.json",
      "/content/person2.json"
    ]
  }
};

let endpointState = {};

let results = stepRunner.runSteps(workUnit, endpointState).toArray();
endpointState = results[0];
let batchResponse = results[1];

let stepResponse = endpointState.job.stepResponses["1"];
let assertions = [
  test.assertEqual("/content/person2.json", endpointState.lastProcessedItem),
  test.assertEqual("job123", endpointState.jobId),

  test.assertEqual(2, stepResponse.totalEvents),
  test.assertEqual(2, stepResponse.failedEvents),
  test.assertEqual(0, stepResponse.successfulBatches),
  test.assertEqual(1, stepResponse.failedBatches,
    "The batch is a failure because all items threw errors"),
  test.assertTrue(stepResponse.stepStartTime != null),

  test.assertEqual("job123", batchResponse.jobId),
  test.assertEqual(2, batchResponse.totalCount),
  test.assertEqual(2, batchResponse.errorCount),
  test.assertEqual(0, batchResponse.completedItems.length),
  test.assertEqual("/content/person1.json", batchResponse.failedItems[0]),
  test.assertEqual("/content/person2.json", batchResponse.failedItems[1]),
  test.assertEqual(2, batchResponse.failedItems.length),
  test.assertEqual(2, batchResponse.errors.length,
    "Oddly, only one error is persisted in the batch document, but all errors are returned in the response object"),
  test.assertEqual("/content/person1.json", batchResponse.errors[0].uri),
  test.assertEqual("/content/person2.json", batchResponse.errors[1].uri)
];

results = stepRunner.runSteps(workUnit, endpointState).toArray();
endpointState = results[0];
batchResponse = results[1];
stepResponse = endpointState.job.stepResponses["1"];

assertions.push(
  test.assertEqual("/content/person3.json", endpointState.lastProcessedItem),
  test.assertEqual("job123", endpointState.jobId),

  test.assertEqual(3, stepResponse.totalEvents),
  test.assertEqual(2, stepResponse.failedEvents),
  test.assertEqual(1, stepResponse.successfulBatches),
  test.assertEqual(1, stepResponse.failedBatches),
  test.assertTrue(stepResponse.stepStartTime != null)
);

results = stepRunner.runSteps(workUnit, endpointState);

job = hubTest.getFirstJobDocument().toObject().job;
stepResponse = job.stepResponses["1"];

assertions.concat(
  test.assertEqual(undefined, results),

  test.assertEqual("job123", job.jobId),
  test.assertEqual("PersonFlow", job.flow),
  test.assertEqual(xdmp.getCurrentUser(), job.user),
  test.assertEqual("failed", job.jobStatus, "If every step has at least one failure, then the job is considered to have failed"),
  test.assertTrue(job.timeStarted != null),
  test.assertTrue(job.timeEnded != null),
  test.assertEqual("1", job.lastAttemptedStep),
  test.assertEqual("1", job.lastCompletedStep),

  test.assertEqual(3, stepResponse.totalEvents),
  test.assertEqual(2, stepResponse.failedEvents),
  test.assertEqual(1, stepResponse.successfulBatches),
  test.assertEqual(1, stepResponse.failedBatches),
  test.assertTrue(stepResponse.stepStartTime != null),
  test.assertEqual("completed step 1", stepResponse.status),
  test.assertTrue(stepResponse.stepEndTime != null)
);

