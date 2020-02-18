const test = require("/test/test-helper.xqy");
const Jobs = require("/data-hub/5/impl/jobs.sjs");
const jobs = new Jobs.Jobs();

assertions = [];

let status = jobs.determineJobStatus({
    job: {
      stepResponses: {
        "1": {
          totalEvents: 3,
          failedEvents: 2
        }
      }
    }
  }
);
assertions.push(test.assertEqual("failed", status,
  "Since there's only one step, and it has failed events, the job is considered to have failed"));

status = jobs.determineJobStatus({
    job: {
      stepResponses: {
        "1": {
          totalEvents: 3,
          failedEvents: 2
        },
        "2": {
          totalEvents: 2,
          failedEvents: 0
        }
      }
    }
  }
);
assertions.push(test.assertEqual("finished_with_errors", status,
  "Since at least one step finished without errors, and at least one step finished with errors, the status is finished_with_errors"));

status = jobs.determineJobStatus({
    job: {
      stepResponses: {
        "1": {
          totalEvents: 3,
          failedEvents: 0
        },
        "2": {
          totalEvents: 2,
          failedEvents: 0
        }
      }
    }
  }
);
assertions.push(test.assertEqual("finished", status,
  "Since no steps had any failures, the job is considered to have successfully finished"));

assertions;
