package org.thingsboard.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

public class RestClientExample {

    private static ObjectMapper mapper = new ObjectMapper();
    private RestClient restClient;

    public static void main(String[] args) throws Exception {
        new RestClientExample().run();
    }

    public void run() throws Exception {
        // ThingsBoard REST API URL
        final String url = "http://localhost:8080";

        // Default System Administrator credentials
        final String username = "sysadmin@thingsboard.org";
        final String password = "sysadmin";

        // creating new rest restClient and auth with system administrator credentials
        restClient = new RestClient(url);
        login(username, password);

        // Creating Tenant
        Tenant tenant = new Tenant();
        tenant.setTitle("Test Tenant");
        tenant = restClient.saveTenant(tenant);

        final String tenantUsername = "testtenant@thingsboard.org";
        final String tenantPassword = "testtenant";

        // Created User for Tenant
        User tenantUser = new User();
        tenantUser.setAuthority(Authority.TENANT_ADMIN);
        tenantUser.setEmail(tenantUsername);
        tenantUser.setTenantId(tenant.getId());

        tenantUser = restClient.saveUser(tenantUser, false);
        restClient.activateUser(tenantUser.getId(), tenantPassword);

        // login with Tenant
        login(tenantUsername, tenantPassword);

        // Loading Widget from file
//        Path widgetFilePath = Paths.get("src/main/resources/custom_widget.json");
//        JsonNode widgetJson = mapper.readTree(Files.readAllBytes(widgetFilePath));
//        loadWidget(widgetJson);

        // Loading Rule Chain from file
//        Path ruleChainFilePath = Paths.get("src/main/resources/rule_chain.json");
//        JsonNode ruleChainJson = mapper.readTree(Files.readAllBytes(ruleChainFilePath));
//        loadRuleChain(ruleChainJson, false);

        // Creating Dashboard Group on the Tenant Level
        EntityGroup sharedDashboardsGroup = new EntityGroup();
        sharedDashboardsGroup.setName("Shared Dashboards");
        sharedDashboardsGroup.setType(EntityType.DASHBOARD);
        sharedDashboardsGroup = restClient.saveEntityGroup(sharedDashboardsGroup);

        // Loading Dashboard from file
        JsonNode dashboardJson = mapper.readTree(RestClientExample.class.getClassLoader().getResourceAsStream("watermeters.json"));
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(dashboardJson.get("title").asText());
        dashboard.setConfiguration(dashboardJson.get("configuration"));
        dashboard = restClient.saveDashboard(dashboard);

        // Adding Dashboard to the Shared Dashboards Group
        restClient.addEntitiesToEntityGroup(sharedDashboardsGroup.getId(), Collections.singletonList(dashboard.getId()));

        // Creating Customer 1
        Customer customer1 = new Customer();
        customer1.setTitle("Customer 1");
        customer1 = restClient.saveCustomer(customer1);

        Device waterMeter1 = new Device();
        waterMeter1.setCustomerId(customer1.getId());
        waterMeter1.setName("WaterMeter1");
        waterMeter1.setType("waterMeter");
        waterMeter1 = restClient.saveDevice(waterMeter1);

        // Update device token
        DeviceCredentials deviceCredentials = restClient.getDeviceCredentialsByDeviceId(waterMeter1.getId()).get();
        deviceCredentials.setCredentialsId("new_device_token");
        restClient.saveDeviceCredentials(deviceCredentials);

        // Fetching automatically created "Customer Administrators" Group.
        EntityGroupInfo customer1Administrators = restClient.getEntityGroupInfoByOwnerAndNameAndType(customer1.getId(), EntityType.USER, "Customer Administrators").get();

        // Creating Read-Only Role
        Role readOnlyRole = restClient.createGroupRole("Read-Only", Arrays.asList(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.READ_CREDENTIALS));

        // Assigning Shared Dashboards to the Customer 1 Administrators
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(readOnlyRole.getId());
        groupPermission.setUserGroupId(customer1Administrators.getId());
        groupPermission.setEntityGroupId(sharedDashboardsGroup.getId());
        groupPermission.setEntityGroupType(sharedDashboardsGroup.getType());
        groupPermission = restClient.saveGroupPermission(groupPermission);

        // Creating User for Customer 1 with default dashboard from Tenant "Shared Dashboards" group.
        String userEmail = "user@thingsboard.org";
        String userPassword = "secret";
        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setCustomerId(customer1.getId());
        user.setEmail(userEmail);
        ObjectNode additionalInfo = mapper.createObjectNode();
        additionalInfo.put("defaultDashboardId", dashboard.getId().toString());
        additionalInfo.put("defaultDashboardFullscreen", false);
        user.setAdditionalInfo(additionalInfo);
        user = restClient.saveUser(user, false);
        restClient.activateUser(user.getId(), userPassword);

        restClient.addEntitiesToEntityGroup(customer1Administrators.getId(), Collections.singletonList(user.getId()));
    }

    private void login(String username, String password) {
        restClient.login(username, password);
    }

    private void loadWidget(JsonNode widgetJson) {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle(widgetJson.get("widgetsBundle").get("title").asText());
        widgetsBundle.setAlias(widgetJson.get("widgetsBundle").get("alias").asText());
        final WidgetsBundle bundle = restClient.saveWidgetsBundle(widgetsBundle);

        WidgetType widgetType = new WidgetType();
        JsonNode widgetTypes = widgetJson.get("widgetTypes");
        widgetTypes.forEach(type -> {
                    widgetType.setName(type.get("alias").asText());
                    widgetType.setAlias(type.get("alias").asText());
                    widgetType.setBundleAlias(bundle.getAlias());
                    widgetType.setDescriptor(type.get("descriptor"));
                    restClient.saveWidgetType(widgetType);
                }
        );
    }

    private void loadRuleChain(JsonNode ruleChainJson, boolean root) throws JsonProcessingException {
        RuleChain ruleChain = mapper.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
        RuleChainMetaData ruleChainMetaData = mapper.treeToValue(ruleChainJson.get("metadata"), RuleChainMetaData.class);
        RuleChain savedRuleChain = restClient.saveRuleChain(ruleChain);
        ruleChainMetaData.setRuleChainId(savedRuleChain.getId());
        restClient.saveRuleChainMetaData(ruleChainMetaData);
        if (root) {
            restClient.setRootRuleChain(savedRuleChain.getId());
        }
    }
}
