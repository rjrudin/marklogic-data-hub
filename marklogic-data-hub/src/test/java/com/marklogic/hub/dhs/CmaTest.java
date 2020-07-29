package com.marklogic.hub.dhs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.mgmt.ManageClient;
import com.marklogic.mgmt.ManageConfig;
import com.marklogic.mgmt.api.configuration.Configuration;
import com.marklogic.mgmt.api.configuration.Configurations;
import com.marklogic.mgmt.api.security.Role;
import com.marklogic.mgmt.resource.databases.DatabaseManager;
import com.marklogic.mgmt.resource.security.RoleManager;

public class CmaTest {

    public static void main(String[] args) throws Exception {
        String json = "{\"database-name\":\"data-hub-FINAL\",\"triple-index\":true}";

        ManageClient client = new ManageClient(new ManageConfig("localhost", 8002, "test-data-hub-developer", "password"));
        new DatabaseManager(client).save(json);

        Configuration config = new Configuration();
        //config.addDatabase((ObjectNode) new ObjectMapper().readTree(json));
        //new Configurations(config).submit(client);

        Role role = new Role("aaa-test-role");
        role.addRole("data-hub-security-admin");
        System.out.println(role.getJson());
        //new RoleManager(client).save(role.getJson());

        config.addRole(role.toObjectNode());
        new Configurations(config).submit(client);
    }
}
