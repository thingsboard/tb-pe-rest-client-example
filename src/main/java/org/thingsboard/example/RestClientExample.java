package org.thingsboard.example;

import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;

public class RestClientExample {
    public static void main(String[] args) {
        // credentials for thingsboard
        String tenantUsername = "tenant@thingsboard.org";
        String tenantPassword = "tenant";

        // url for thingsboard
        String url = "http://localhost:8080";

        // creating new rest client and auth with credentials
        RestClient client = new RestClient(url);
        client.login(tenantUsername, tenantPassword);

        // creating customer
        Customer customer = new Customer();
        customer.setTitle("Customer_1");
        customer = client.saveCustomer(customer);

        // creating user for customer
        String userEmail = "user@thingsboard.org";
        String userPassword = "user";
        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setCustomerId(customer.getId());
        user.setEmail(userEmail);
        user = client.saveUser(user, false);

        // activation user
        client.activateUser(user.getId(), userPassword);

        // creating dashboard
        Dashboard dashboard = new Dashboard();
    }
}
