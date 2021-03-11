package com.marklogic.hub.step.impl;

import com.marklogic.client.io.marker.AbstractWriteHandle;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ContentIngester {

    void initialize(IngestionInputs ingestionInputs);

    void ingest(String uri, AbstractWriteHandle content);

    void awaitCompletion();

    void abort();

    void setSuccessListener(Consumer<String[]> urisListener);

    void setFailureListener(BiConsumer<String[], Throwable> urisAndErrorListener);
}
