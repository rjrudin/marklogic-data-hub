package com.marklogic.hub.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.marklogic.appdeployer.command.Command;
import com.marklogic.appdeployer.command.security.DeployAmpsCommand;
import com.marklogic.appdeployer.command.security.DeployPrivilegesCommand;
import com.marklogic.appdeployer.command.triggers.DeployTriggersCommand;
import com.marklogic.hub.deploy.HubAppDeployer;
import com.marklogic.hub.deploy.commands.DeployDatabaseFieldCommand;
import com.marklogic.hub.impl.DataHubImpl;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.mgmt.resource.hosts.HostManager;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Parameters(commandDescription = "Install or upgrade DHF into a DHS environment")
public class InstallDhfInDhsCommand extends AbstractInstallerCommand {

    @Parameter(
        names = {"-h", "--hostname"},
        required = true,
        description = "Name of the MarkLogic host corresponding to the value of mlHost; this is used for changing groups so that query options can be loaded into each relevant app server"
    )
    private String hostname;

    private DataHubImpl dataHub;
    private HubConfigImpl hubConfig;

    public InstallDhfInDhsCommand(DataHubImpl dataHub, HubConfigImpl hubConfig) {
        this.dataHub = dataHub;
        this.hubConfig = hubConfig;
    }

    @Override
    public void run(Options options) {
        logger.info("Installing DHF version " + hubConfig.getJarVersion());

        initializeProject(options);

        HubAppDeployer finalDeployer = new HubAppDeployer(
            hubConfig.getManageClient(), hubConfig.getAdminManager(), null, hubConfig.newStagingClient());

        logger.info("Getting current group for host: " + hostname);
        final String originalGroupName = new HostManager(hubConfig.getManageClient()).getAssignedGroupName(hostname);
        logger.info(format("Current group is %s; will set host %s back to that group after DHF is deployed",
            originalGroupName, hostname));

        try {
            String groupName = "Evaluator";
            modifyHubConfigForDhs(groupName);
            setHostToGroup(hubConfig, groupName);
            finalDeployer.setCommands(buildCommandsForDhs());
            finalDeployer.deploy(hubConfig.getAppConfig());

            groupName = "Curator";
            modifyHubConfigForDhs(groupName);
            setHostToGroup(hubConfig, groupName);
            finalDeployer.setCommands(buildCommandsForDhsForServesAndModules());
            finalDeployer.deploy(hubConfig.getAppConfig());
        } finally {
            setHostToGroup(hubConfig, originalGroupName);
        }
    }

    /**
     * In the spirit of whitelisting, we'll only setup the commands that we know we need for installing DHF.
     * We may need a more broad set of commands for user files.
     */
    protected List<Command> buildCommandsForDhs() {
        List<Command> commands = new ArrayList<>();
        commands.add(new DeployPrivilegesCommand());
        commands.add(new DeployAmpsCommand());
        commands.add(new DhsDeployDatabasesCommand(hubConfig));
        commands.add(new DhsDeployServersCommand());
        commands.add(new DeployTriggersCommand());
        commands.add(new DeployDatabaseFieldCommand());

        Map<String, List<Command>> commandMap = dataHub.buildCommandMap();
        commands.addAll(commandMap.get("mlModuleCommands"));

        return commands;
    }

    protected List<Command> buildCommandsForDhsForServesAndModules() {
        List<Command> commands = new ArrayList<>();
        commands.add(new DhsDeployServersCommand());
        Map<String, List<Command>> commandMap = dataHub.buildCommandMap();
        commands.addAll(commandMap.get("mlModuleCommands"));
        return commands;
    }

    protected void modifyHubConfigForDhs(String groupName) {
        // DHS will handle all forest creation
        hubConfig.getAppConfig().setCreateForests(false);
        hubConfig.getAppConfig().setGroupName(groupName);
        hubConfig.getAppConfig().setDeployPrivilegesWithCma(true);
        hubConfig.getAppConfig().setDeployAmpsWithCma(true);
    }

    /**
     * The intended use case is that an executable DHF jar can be run from any directory, which means we need to first
     * initialize a DHF project (specifically, generating the gradle.properties file) and then refresh HubConfig based
     * on those properties and anything a client passed in via JVM props.
     *
     * @param options
     */
    protected void initializeProject(Options options) {
        // Include System properties so that a client can override e.g. mlHost/mlUsername/mlPassword via JVM props
        Properties props = new Properties();
        for (String key : System.getProperties().stringPropertyNames()) {
            props.put(key, System.getProperties().getProperty(key));
        }

        // Properties required for DHS
        props.setProperty("mlIsHostLoadBalancer", "true");
        props.setProperty("mlIsProvisionedEnvironment", "true");

        // Overrides for DHS
        props.setProperty("mlFlowDeveloperRole", "flowDeveloper");
        props.setProperty("mlFlowOperatorRole", "flowOperator");
        // TODO Don't know what to do with data-hub-admin, just mapping it to flowDeveloper for now
        props.setProperty("mlDataHubAdminRole", "flowDeveloper");
        props.setProperty("mlModulePermissions",
            "rest-reader,read,rest-writer,insert,rest-writer,update,rest-extension-user,execute,flowDeveloper,read,flowDeveloper,execute,flowDeveloper,insert,flowOperator,read,flowOperator,execute");

        initializeProject(hubConfig, options, props);
    }

    protected void setHostToGroup(HubConfigImpl hubConfig, String groupName) {
        logger.info(String.format("Setting group to '%s' for host '%s'", groupName, hostname));
        ResponseEntity<String> response = new HostManager(hubConfig.getManageClient()).setHostToGroup(hostname, groupName);
        if (response != null && response.getHeaders().getLocation() != null) {
            hubConfig.getAdminManager().waitForRestart();
        }
        logger.info(String.format("Finished setting group to '%s' for host '%s'", groupName, hostname));
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
