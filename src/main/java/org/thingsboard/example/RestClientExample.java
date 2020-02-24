package org.thingsboard.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.security.Authority;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

public class RestClientExample {

    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        // ThingsBoard REST API URL
        String url = "http://localhost:8080";

        // Default Tenant Administrator credentials
        String username = "tenant@thingsboard.org";
        String password = "tenant";

        // creating new rest restClient and auth with credentials
        RestClient restClient = new RestClient(url);
        restClient.login(username, password);

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
        restClient.saveDevice(waterMeter1);

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
}
