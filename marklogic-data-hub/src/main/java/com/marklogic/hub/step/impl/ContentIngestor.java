package com.marklogic.hub.step.impl;

import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.marker.AbstractWriteHandle;
import com.marklogic.hub.HubClient;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ContentIngestor {

    void initialize(HubClient hubClient, IngestionInputs ingestionInputs);

    void ingest(String uri, InputStreamHandle content);

    void awaitCompletion();

    void abort();

    void setSuccessListener(Consumer<String[]> urisListener);

    void setFailureListener(BiConsumer<String[], Throwable> urisAndErrorListener);
}
