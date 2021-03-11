package com.marklogic.hub.step.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.dataservices.IOEndpoint;
import com.marklogic.client.dataservices.InputCaller;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.marker.AbstractWriteHandle;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.HubClient;

import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BulkContentIngestor implements ContentIngestor {

    private InputCaller.BulkInputCaller<InputStream> bulkInputCaller;

    @Override
    public void initialize(HubClient hubClient, IngestionInputs inputs) {
        DatabaseClient databaseClient = inputs.getDatabase().equals(hubClient.getDbName(DatabaseKind.FINAL)) ?
            hubClient.getFinalClient() :
            hubClient.getStagingClient();

        // TODO This won't work for CSV
        InputCaller<InputStream> inputCaller = InputCaller.on(
            databaseClient, null, new InputStreamHandle());
        IOEndpoint.CallContext callContext = inputCaller.newCallContext();

        // TODO Dump a bunch of stuff into this as endpointConstants
        this.bulkInputCaller = inputCaller.bulkCaller(callContext);
    }

    @Override
    public void ingest(String uri, InputStreamHandle content) {
        // Weird - so we ignore the URI and the format on the content?
    }

    @Override
    public void awaitCompletion() {

    }

    @Override
    public void abort() {

    }

    @Override
    public void setSuccessListener(Consumer<String[]> urisListener) {

    }

    @Override
    public void setFailureListener(BiConsumer<String[], Throwable> urisAndErrorListener) {

    }
}
