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
import java.util.stream.Stream;

public class IngestAndMasterPersons {

    private final static Logger logger = LoggerFactory.getLogger(IngestAndMasterPersons.class);

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        JCommander commander = JCommander.newBuilder().addObject(options).build();
        commander.parse(args);

        logger.info("Will connect to host: " + options.getHost());
        logger.info("Person count: " + options.getPersonCount());
        logger.info("Name randomness: " + options.getNameRandomness());
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

        logger.info("Person count: " + options.getPersonCount());
        Stream.of("sm-Person-mastered", "sm-Person-merged", "sm-Person-archived", "sm-Person-notification", "sm-Person-auditing",
            "http://marklogic.com/Person-0.0.1/Person", "Person").forEach(collection -> {
            logger.info(collection + ": " + hubClient.getFinalClient().newServerEval()
                .javascript("cts.estimate(cts.collectionQuery('" + collection + "'))").evalAs(String.class));
        });
    }

    private static void ingestPersons(HubClient hubClient, Options options) throws Exception {
        DataMovementManager dmm = hubClient.getStagingClient().newDataMovementManager();
        WriteBatcher writeBatcher = dmm.newWriteBatcher()
            .withThreadCount(24)
            .withBatchSize(100)
            .withTransform(new ServerTransform("mlRunIngest").addParameter("flow-name", "persons").addParameter("step", "1"));

        PersonGenerator personGenerator = new PersonGenerator(new File("data/persons"), options);
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

class PersonGenerator {

    private List<String> firstNames = new ArrayList<>();
    private List<String> lastNames = new ArrayList<>();

    private ObjectMapper objectMapper = new ObjectMapper();
    private Random random = new Random(System.currentTimeMillis());
    private Options options;

    public PersonGenerator(File personDir, Options options) throws IOException {
        this.options = options;

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
        int first = random.nextInt(899) + 100;
        int second = random.nextInt(9) + 1;
        int third = random.nextInt(9) + 1;
        return "" + first + "-" + second + second + "-" + third + third + third + third;
    }

    private String makeDob() {
        int year = random.nextInt(100) + 1900;
        String month = "0" + (random.nextInt(9) + 1);
        return year + "-" + month + "-01";
    }

    private String makeAddress() {
        int number = random.nextInt(1111) + 8888;
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
            int numberOfNames = options.getPersonCount() / options.getNameRandomness();
            for (int i = 0; i < numberOfNames; i++) {
                lastNames.add(name + i);
            }
        }
    }

    private void addFirstNames(ObjectNode node) {
        if (node.has("FirstName")) {
            String name = node.get("FirstName").asText();
            firstNames.add(name);
            int numberOfNames = options.getPersonCount() / options.getNameRandomness();
            for (int i = 0; i < numberOfNames; i++) {
                firstNames.add(name + i);
            }
        }
    }
}
