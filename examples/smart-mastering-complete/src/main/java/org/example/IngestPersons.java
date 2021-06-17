package org.example;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.datamovement.DataMovementManager;
import com.marklogic.client.datamovement.WriteBatcher;
import com.marklogic.client.document.DocumentWriteOperation;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.impl.DocumentWriteOperationImpl;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.hub.HubClient;
import com.marklogic.hub.HubClientConfig;
import com.marklogic.hub.flow.FlowInputs;
import com.marklogic.hub.flow.FlowRunner;
import com.marklogic.hub.flow.RunFlowResponse;
import com.marklogic.hub.flow.impl.FlowRunnerImpl;
import com.marklogic.hub.impl.DataHubImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class IngestPersons {

    private final static Logger logger = LoggerFactory.getLogger(IngestPersons.class);

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        JCommander commander = JCommander.newBuilder().addObject(options).build();
        commander.parse(args);

        logger.info("Will connect to host: " + options.getHost());
        logger.info("Person count: " + options.getPersonCount());
        logger.info("Will run steps: " + options.getSteps());

        Properties props = new Properties();
        props.setProperty("mlHost", options.getHost());
        props.setProperty("mlUsername", "pari");
        props.setProperty("mlPassword", "password");
        HubClient hubClient = HubClient.withHubClientConfig(new HubClientConfig(props));

        if (options.isClearData()) {
            new DataHubImpl(hubClient).clearUserData();
        }

        if (options.getPersonCount() > 0) {
            ingestPersons(hubClient, options);
        }

        FlowRunner flowRunner = new FlowRunnerImpl(hubClient);
        FlowInputs flowInputs = new FlowInputs("persons");
        flowInputs.setSteps(Arrays.asList(options.getSteps().split(",")));

        RunFlowResponse response = flowRunner.runFlow(flowInputs);
        flowRunner.awaitCompletion();
        logger.info(response.toJson());
    }

    private static void ingestPersons(HubClient hubClient, Options options) throws Exception {
        DataMovementManager dmm = hubClient.getStagingClient().newDataMovementManager();
        WriteBatcher writeBatcher = dmm.newWriteBatcher()
            .withThreadCount(24)
            .withBatchSize(100)
            .withTransform(new ServerTransform("mlRunIngest").addParameter("flow-name", "persons").addParameter("step", "1"));

        PersonGenerator personGenerator = new PersonGenerator(new File("data/persons"));
        DocumentMetadataHandle metadata = new DocumentMetadataHandle();
        metadata.withCollections("ingest-persons")
            .withPermission("data-hub-common", DocumentMetadataHandle.Capability.READ, DocumentMetadataHandle.Capability.UPDATE);

        long start = System.currentTimeMillis();
        for (int i = 1; i <= options.getPersonCount(); i++) {
            ObjectNode node = personGenerator.newPerson(i);
            writeBatcher.add(new DocumentWriteOperationImpl(DocumentWriteOperation.OperationType.DOCUMENT_WRITE,
                "/person" + i + ".json", metadata, new JacksonHandle(node)));
        }
        writeBatcher.flushAndWait();
        dmm.stopJob(writeBatcher);
        logger.info("Finished ingesting persons, time: " + (System.currentTimeMillis() - start));
    }

}

/**
 * 45-56 SSN
 * 49-65 SSN
 * 22-6 SSN
 * 91-47 Custom DOB, LastName, DM
 * 21-51-85 (51-21 SSN; no mention of 85 though)
 *
 * 95-4 Custom DOB, DM
 * 63-94 SSN
 * 81-67 Custom DOB, DM
 * 91-47 Custom DOB, LastName, DM
 * 21-51-85 (85-21 LastName, DM) - /com.marklogic.smart-mastering/merged/2045eaf4a7eb71f6ef530768e2932c0d.json
 *
 * 13-3-38 SSN; and 13 is mapped to both 3 and 38
 * 22-6 SSN
 * 21-51-85 SSN, then LastName + DM
 *
 * 44-8 Custom DOB, DM, and 44-79 LastName + DB, AND 30-78 LastName + DM + 30-79 SSN
 * - So this has 30, 44, 78, 79, 8
 * - Really weird one
 * 3-13 SSN, and 3-38 SSN, and 38-13 SSN; so 13-3-38
 *
 * Got the whole 30-44-78-79-8
 * 49-65 SSN
 * 63-94 SSN
 *
 * UNIQUE LIST
 * 45-56
 * 49-65
 * 22-6
 * 91-47
 * 21-51-85
 * 95-4
 * 63-94
 * 81-67
 * 13-3-38
 * 8-30-44-78-79
 */
class PersonGenerator {

    private List<String> firstNames = new ArrayList<>();
    private List<String> lastNames = new ArrayList<>();

    private ObjectMapper objectMapper = new ObjectMapper();
    private Random random = new Random(System.currentTimeMillis());

    public PersonGenerator(File personDir) throws IOException {
        for (File file : personDir.listFiles((dir, name) -> name.endsWith(".json"))) {
            ObjectNode node = (ObjectNode) objectMapper.readTree(file);
            addFirstNames(node);
            addLastNames(node);
        }
    }

    public ObjectNode newPerson(int id) {
        ObjectNode person = objectMapper.createObjectNode();
        person.put("PersonID", id + "");
        person.put("FirstName", firstNames.get(random.nextInt(firstNames.size())));
        person.put("LastName", lastNames.get(random.nextInt(lastNames.size())));
        person.put("SSN", makeSsn());
        person.put("DateOfBirth", makeDob());
        person.put("Address", makeAddress());
        person.put("ZipCode", makeZip());
        return person;
    }

    private String makeSsn() {
        int first = random.nextInt(9) + 1;
        int second = random.nextInt(9) + 1;
        int third = random.nextInt(9) + 1;
        return "" + first + first + first + "-" + second + second + "-" + third + third + third + third;
    }

    private String makeDob() {
        int year = random.nextInt(50) + 1950;
        String month = "0" + (random.nextInt(9) + 1);
        return year + "-" + month + "-01";
    }

    private String makeAddress() {
        int number = random.nextInt(111) + 888;
        return number + " Main St";
    }

    private String makeZip() {
        int zip = random.nextInt(88888) + 11111;
        return "" + zip;
    }

    private void addLastNames(ObjectNode node) {
        if (node.has("LastName")) {
            String name = node.get("LastName").asText();
            lastNames.add(name);
            for (int i = 0; i < 10; i++) {
                lastNames.add(name + i);
            }
        }
    }

    private void addFirstNames(ObjectNode node) {
        if (node.has("FirstName")) {
            String name = node.get("FirstName").asText();
            firstNames.add(name);
            for (int i = 0; i < 10; i++) {
                firstNames.add(name + i);
            }

        }
    }
}
