/*
 * Copyright 2020 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.hub.cloud.aws.glue.Writer;

import com.marklogic.hub.HubClient;
import com.marklogic.hub.impl.HubConfigImpl;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.sources.v2.writer.DataWriter;
import org.apache.spark.sql.sources.v2.writer.DataWriterFactory;
import org.apache.spark.sql.types.StructType;

import java.util.Map;
import java.util.Properties;

public class MarkLogicDataWriterFactory implements DataWriterFactory<InternalRow> {

    private StructType schema;
    private HubClient hubClient;
    private Map<String, String> params;

    public MarkLogicDataWriterFactory(Map<String, String> params, StructType schema) {
        this.params = params;
        this.schema = schema;

        Properties props = new Properties();
        props.setProperty("mlHost", params.get("host"));
        // It's assumed that the user wants to write to staging, but we may need to make this more configurable
        // e.g. this may need to be "staging" or "final", not a port number
        props.setProperty("mlStagingPort", params.get("port"));
        props.setProperty("mlUsername", params.get("user"));
        props.setProperty("mlPassword", params.get("password"));
        props.setProperty("mlModulesDbName", params.get("modulesdatabase"));

        // This assumes the use of DHS. We may need to externalize this so that a user has the option of talking to an
        // ML instance that is not a DHS one
        props.setProperty("hubDhs", "true");
        props.setProperty("hubSsl", "true");
        this.hubClient = HubConfigImpl.withProperties(props).newHubClient();
    }

    @Override
    public DataWriter<InternalRow> createDataWriter(int partitionId, long taskId, long epochId) {
        System.out.println("************** task id ************** "+ taskId);
        return new MarkLogicDataWriter(hubClient, taskId, schema, params);
    }
}
