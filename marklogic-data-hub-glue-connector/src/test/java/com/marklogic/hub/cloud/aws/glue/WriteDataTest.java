package com.marklogic.hub.cloud.aws.glue;

import com.marklogic.client.DatabaseClient;
import com.marklogic.hub.cloud.aws.glue.Writer.MarkLogicDataWriter;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class WriteDataTest extends AbstractGlueConnectorTest {

    /**
     * First cut at a test that passes a HubClient to MarkLogicDataWriter and writes some rows, then verifies the
     * results.
     */
    @Test
    void test() {
        StructType schema = null; // TODO Set this to something for the test
        Map<String, String> params = new HashMap<>();
        params.put("batchsize", "10");
        params.put("apipath", "TODO");
        params.put("prefixvalue", "TODO");

        MarkLogicDataWriter writer = new MarkLogicDataWriter(getHubClient(), 123, schema, params);
        InternalRow row1 = null; // TODO Set this to something
        InternalRow row2 = null; // TODO Set this to something
        writer.write(row1);
        writer.write(row2);

        DatabaseClient stagingClient = getHubClient().getStagingClient();
        // TODO Verify that the results were written to the staging database correctly
    }

}
