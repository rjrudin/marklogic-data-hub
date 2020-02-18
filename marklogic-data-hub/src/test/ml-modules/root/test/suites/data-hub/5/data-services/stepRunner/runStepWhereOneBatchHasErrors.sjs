const StepRunner = require("/data-hub/5/impl/step-runner.sjs");
const test = require("/test/test-helper.xqy");

const stepRunner = new StepRunner();

const workUnit = {
  flowName: "PersonFlow",
  stepNumbers: ["1"],
  batchSize: 2,
  jobId: "job123",
  options: {
    failForUris: ["/content/person2.json"]
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
  test.assertEqual(1, stepResponse.failedEvents),
  test.assertEqual(1, stepResponse.successfulBatches,
    "The batch is considered successful since not every item failed"),
  test.assertEqual(0, stepResponse.failedBatches),
  test.assertTrue(stepResponse.stepStartTime != null),

  test.assertEqual("job123", batchResponse.jobId),
  test.assertEqual(2, batchResponse.totalCount),
  test.assertEqual(1, batchResponse.errorCount),
  test.assertEqual("/content/person1.json", batchResponse.completedItems[0]),
  test.assertEqual(1, batchResponse.completedItems.length),
  test.assertEqual("/content/person2.json", batchResponse.failedItems[0]),
  test.assertEqual(1, batchResponse.failedItems.length),
  test.assertEqual("Intentionally throwing error for URI: /content/person2.json", batchResponse.errors[0].message),
  test.assertEqual("/content/person2.json", batchResponse.errors[0].uri)
];

results = stepRunner.runSteps(workUnit, endpointState).toArray();
endpointState = results[0];
batchResponse = results[1];
stepResponse = endpointState.job.stepResponses["1"];

assertions.push(
  test.assertEqual("/content/person3.json", endpointState.lastProcessedItem),
  test.assertEqual("job123", endpointState.jobId),

  test.assertEqual(3, stepResponse.totalEvents),
  test.assertEqual(1, stepResponse.failedEvents),
  test.assertEqual(2, stepResponse.successfulBatches),
  test.assertEqual(0, stepResponse.failedBatches),
  test.assertTrue(stepResponse.stepStartTime != null)
);

results = stepRunner.runSteps(workUnit, endpointState);
assertions.concat(test.assertEqual(undefined, results));
