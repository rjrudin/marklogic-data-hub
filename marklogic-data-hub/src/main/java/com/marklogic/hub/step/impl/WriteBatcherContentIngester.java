package com.marklogic.hub.step.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.datamovement.*;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.ext.helper.LoggingObject;
import com.marklogic.client.ext.util.DefaultDocumentPermissionsParser;
import com.marklogic.client.ext.util.DocumentPermissionsParser;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.marker.AbstractWriteHandle;
import com.marklogic.client.io.marker.BufferableContentHandle;
import com.marklogic.client.io.marker.ContentHandle;
import com.marklogic.hub.DatabaseKind;
import com.marklogic.hub.HubClient;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WriteBatcherContentIngester extends LoggingObject implements ContentIngester {

    private static final String DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private DataMovementManager dataMovementManager;
    private WriteBatcher writeBatcher;
    private String username;

    private Consumer<String[]> successListener;
    private BiConsumer<String[], Throwable> failureListener;

    private DocumentPermissionsParser documentPermissionsParser = new DefaultDocumentPermissionsParser();

    public WriteBatcherContentIngester(HubClient hubClient, String targetDatabase) {
        this.username = hubClient.getUsername();
        dataMovementManager = targetDatabase.equals(hubClient.getDbName(DatabaseKind.FINAL)) ?
            hubClient.getFinalClient().newDataMovementManager() :
            hubClient.getStagingClient().newDataMovementManager();
    }

    @Override
    public void initialize(IngestionInputs inputs) {
        ServerTransform serverTransform = new ServerTransform("mlRunIngest");
        serverTransform.addParameter("job-id", inputs.getJobId());
        serverTransform.addParameter("step", inputs.getStepNumber());
        serverTransform.addParameter("flow-name", inputs.getFlowName());
        String optionString = jsonToString(inputs.getOptions());
        serverTransform.addParameter("options", optionString);

        this.writeBatcher = dataMovementManager.newWriteBatcher()
            .withBatchSize(inputs.getBatchSize())
            .withThreadCount(inputs.getThreadCount())
            .withJobId(inputs.getJobId())
            .withTransform(serverTransform)
            .onBatchSuccess(this::onBatchSuccess)
            .onBatchFailure(this::onBatchFailure);

        DocumentMetadataHandle metadataHandle = new DocumentMetadataHandle();
        if (StringUtils.isNotEmpty(inputs.getPermissions())) {
            documentPermissionsParser.parsePermissions(inputs.getPermissions(), metadataHandle.getPermissions());
        }
        if (StringUtils.isNotEmpty(inputs.getCollections())) {
            metadataHandle.withCollections(inputs.getCollections().split(","));
        }
        if ("default-ingestion".equals(inputs.getFlowName())) {
            metadataHandle.withCollections("default-ingestion");
        }

        DocumentMetadataHandle.DocumentMetadataValues metadataValues = metadataHandle.getMetadataValues();
        metadataValues.add("datahubCreatedByJob", inputs.getJobId());
        metadataValues.add("datahubCreatedInFlow", inputs.getFlowName());
        metadataValues.add("datahubCreatedByStep", inputs.getStepName());
        // createdOn/createdBy data may not be accurate enough. Unfortunately REST transforms don't allow for writing metadata
        metadataValues.add("datahubCreatedOn", new SimpleDateFormat(DATE_TIME_FORMAT_PATTERN).format(new Date()));
        metadataValues.add("datahubCreatedBy", this.username);
        writeBatcher.withDefaultMetadata(metadataHandle);
        this.dataMovementManager.startJob(writeBatcher);
    }

    @Override
    public void ingest(String uri, AbstractWriteHandle contentHandle) {
        if (!writeBatcher.isStopped()) {
            try {
                writeBatcher.add(uri, contentHandle);
            } catch (IllegalStateException ex) {
                logger.warn("Cannot ingest file, WriteBatcher has been stopped");
            }
        }
    }

    @Override
    public void awaitCompletion() {
        if (this.writeBatcher != null) {
            try {
                this.writeBatcher.flushAndWait();
            } catch (IllegalStateException ex) {
                logger.debug("WriteBatcher has been stopped");
            }
            this.dataMovementManager.stopJob(this.writeBatcher);
        }
    }

    @Override
    public void abort() {
        if (this.dataMovementManager != null && this.writeBatcher != null && !this.writeBatcher.isStopped()) {
            this.dataMovementManager.stopJob(this.writeBatcher);
        }
    }

    private void onBatchSuccess(WriteBatch batch) {
        String[] uris = new String[batch.getItems().length];
        for (int i = 0; i < uris.length; i++) {
            uris[i] = batch.getItems()[i].getTargetUri();
        }
        successListener.accept(uris);
    }

    private void onBatchFailure(WriteBatch batch, Throwable failure) {
        String[] uris = new String[batch.getItems().length];
        for (int i = 0; i < uris.length; i++) {
            uris[i] = batch.getItems()[i].getTargetUri();
        }
        failureListener.accept(uris, failure);
    }

    private String jsonToString(Map map) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(objectMapper.convertValue(map, JsonNode.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSuccessListener(Consumer<String[]> successListener) {
        this.successListener = successListener;
    }

    public void setFailureListener(BiConsumer<String[], Throwable> failureListener) {
        this.failureListener = failureListener;
    }
}
