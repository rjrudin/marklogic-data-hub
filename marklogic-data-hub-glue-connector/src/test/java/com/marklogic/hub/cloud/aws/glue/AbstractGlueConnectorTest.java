package com.marklogic.hub.cloud.aws.glue;

import com.marklogic.hub.HubClient;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.hub.test.AbstractHubTest;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;

/**
 * Base class for all glue-connector tests. This assumes that we want to run every test as a data-hub-operator user.
 */
public class AbstractGlueConnectorTest extends AbstractHubTest {

    private HubConfigImpl hubConfig;
    private HubClient hubClient;

    @BeforeEach
    void beforeEachTest() {
        hubConfig = new HubConfigImpl();
        resetDatabases();
        runAsDataHubOperator();
    }

    @Override
    protected HubClient getHubClient() {
        return hubClient;
    }

    @Override
    protected HubConfigImpl getHubConfig() {
        return hubConfig;
    }

    @Override
    protected File getTestProjectDirectory() {
        // We don't have any need for a HubProject within the connector
        return null;
    }

    @Override
    protected HubConfigImpl runAsUser(String username, String password) {
        applyMlUsernameAndMlPassword(username, password);
        this.hubClient = hubConfig.newHubClient();
        return hubConfig;
    }
}
