package com.marklogic.hub.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.appdeployer.AppConfig;
import com.marklogic.bootstrap.Installer;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.hub.ApplicationConfig;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.HubTestBase;
import com.marklogic.hub.flow.impl.FlowRunnerImpl;
import com.marklogic.hub.impl.SimpleHubConfig;
import com.marklogic.hub.step.MarkLogicStepDefinitionProvider;
import com.marklogic.hub.step.StepDefinition;
import com.marklogic.hub.step.StepDefinitionProvider;
import com.marklogic.hub.step.StepRunnerFactory;
import com.marklogic.mgmt.util.ObjectMapperFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ApplicationConfig.class)
public class RunFlowWithoutProjectTest extends HubTestBase {

    @BeforeAll
    public static void setup() {
        new Installer().deleteProjectDir();
    }

    @AfterAll
    public static void tearDown() {
        new Installer().deleteProjectDir();
    }

    @Test
    public void test() throws Exception {
        setupProjectForRunningTestFlow();
        makeInputFilePathsAbsoluteInFlow();

        SimpleHubConfig hubConfig = new SimpleHubConfig();
        AppConfig appConfig = new AppConfig();
        appConfig.setHost(host);
        hubConfig.setHost(host);
        hubConfig.setAppConfig(appConfig);
        hubConfig.setMlUsername(flowRunnerUser);
        hubConfig.setMlPassword(flowRunnerPassword);

        StepRunnerFactory stepRunnerFactory = new StepRunnerFactory(hubConfig);
        StepDefinitionProvider provider = new MarkLogicStepDefinitionProvider(hubConfig.newStagingClient());
        verifyThatStepDefinitionsCanBeFound(provider);
        stepRunnerFactory.setStepDefinitionProvider(provider);

        FlowRunnerImpl flowRunner = new FlowRunnerImpl(hubConfig, stepRunnerFactory);
        FlowInputs inputs = new FlowInputs("testFlow");
        flowRunner.runFlow(inputs);
        flowRunner.awaitCompletion();

        assertEquals(getDocCount(HubConfig.DEFAULT_STAGING_NAME, "xml-coll"), 1);
        assertEquals(getDocCount(HubConfig.DEFAULT_STAGING_NAME, "csv-coll"), 25);
        assertEquals(getDocCount(HubConfig.DEFAULT_STAGING_NAME, "csv-tab-coll"), 25);
        assertEquals(getDocCount(HubConfig.DEFAULT_FINAL_NAME, "xml-map"), 1);
    }

    /**
     * This is needed because if the paths are relative (which they are by default), then a HubProject is needed.
     *
     * @throws Exception
     */
    private void makeInputFilePathsAbsoluteInFlow() throws Exception {
        Path projectDir = adminHubConfig.getHubProject().getProjectDir();
        final String inputDir = projectDir.resolve("input").toFile().getAbsolutePath();
        final File flowFile = projectDir.resolve("flows").resolve("testFlow.flow.json").toFile();
        ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
        JsonNode flow = objectMapper.readTree(flowFile);
        ((ObjectNode) flow.get("steps").get("1").get("fileLocations")).put("inputFilePath", inputDir);
        ((ObjectNode) flow.get("steps").get("3").get("fileLocations")).put("inputFilePath", inputDir + "/csv");
        ((ObjectNode) flow.get("steps").get("4").get("fileLocations")).put("inputFilePath", inputDir + "/tabs");
        ObjectMapperFactory.getObjectMapper().writeValue(flowFile, flow);
        JSONDocumentManager mgr = stagingClient.newJSONDocumentManager();
        DocumentMetadataHandle metadata = mgr.readMetadata("/flows/testFlow.flow.json", new DocumentMetadataHandle());
        mgr.write("/flows/testFlow.flow.json", metadata, new JacksonHandle(flow));
    }

    /**
     * Make sure that MarkLogicStepDefinitionProvider works for default and custom steps.
     *
     * @param provider
     */
    private void verifyThatStepDefinitionsCanBeFound(StepDefinitionProvider provider) {
        StepDefinition stepDef = provider.getStepDefinition("default-mapping", StepDefinition.StepDefinitionType.MAPPING);
        assertEquals("default-mapping", stepDef.getName());

        stepDef = provider.getStepDefinition("json-mapping", StepDefinition.StepDefinitionType.MAPPING);
        assertEquals("json-mapping", stepDef.getName());
    }
}
