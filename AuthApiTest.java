package com.bugtracker.automation.api;

import com.bugtracker.automation.base.BaseApiTest;
import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import com.bugtracker.automation.utils.TestDataFactory;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class AuthApiTest extends BaseApiTest {

    @Test(groups = {"smoke", "api", "auth"}, description = "Register returns 201 and a JWT (3 dot-separated parts)")
    public void registerReturnsJwt() {
        String email = TestDataFactory.uniqueEmail();

        Response response = api.register(TestDataFactory.personName(), email, "Passw0rd!", "DEVELOPER");

        assertEquals(response.statusCode(), 201);
        assertEquals(response.jsonPath().getString("token").split("\\.").length, 3, "JWT has header.payload.signature");
        assertEquals(response.jsonPath().getString("email"), email);
        assertEquals(response.jsonPath().getString("role"), "DEVELOPER");
        assertNotNull(response.jsonPath().get("userId"));
    }

    @Test(groups = {"regression", "api", "auth"}, description = "Duplicate email -> 400")
    public void duplicateEmailReturns400() {
        TestUser existing = api.registerUser(Role.DEVELOPER);

        Response response = api.register(TestDataFactory.personName(), existing.getEmail(), "Passw0rd!", "DEVELOPER");

        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("message"), "Email already registered");
    }

    @Test(groups = {"regression", "api", "auth"}, description = "Password boundary: 5 chars rejected with a field error")
    public void shortPasswordReturns400() {
        Response response = api.register(TestDataFactory.personName(), TestDataFactory.uniqueEmail(), "12345", "DEVELOPER");

        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("errors.password"), "Password must be at least 6 characters");
    }

    @Test(groups = {"regression", "api", "auth"}, description = "Password boundary: exactly 6 chars accepted")
    public void sixCharPasswordReturns201() {
        Response response = api.register(TestDataFactory.personName(), TestDataFactory.uniqueEmail(), "123456", "DEVELOPER");

        assertEquals(response.statusCode(), 201);
    }

    @Test(groups = {"regression", "api", "auth"}, description = "Blank name and missing role are reported per field")
    public void missingFieldsReturnFieldErrors() {
        Response response = api.register("", TestDataFactory.uniqueEmail(), "Passw0rd!", null);

        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("errors.name"), "Name is required");
        assertEquals(response.jsonPath().getString("errors.role"), "Role is required");
    }

    @Test(groups = {"regression", "api", "auth"}, description = "Malformed email -> 400")
    public void invalidEmailFormatReturns400() {
        Response response = api.register(TestDataFactory.personName(), "not-an-email", "Passw0rd!", "DEVELOPER");

        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("errors.email"), "Email must be valid");
    }

    @Test(groups = {"smoke", "api", "auth"}, description = "Valid login returns 200 and a token")
    public void loginReturnsToken() {
        TestUser user = api.registerUser(Role.TEAM_LEAD);

        Response response = api.login(user.getEmail(), user.getPassword());

        assertEquals(response.statusCode(), 200);
        assertNotNull(response.jsonPath().getString("token"));
        assertEquals(response.jsonPath().getString("role"), "TEAM_LEAD");
    }

    @Test(groups = {"regression", "api", "auth"}, description = "Wrong password -> 401")
    public void wrongPasswordReturns401() {
        TestUser user = api.registerUser(Role.DEVELOPER);

        Response response = api.login(user.getEmail(), "WrongPass123");

        assertEquals(response.statusCode(), 401);
        assertEquals(response.jsonPath().getString("message"), "Invalid email or password");
    }

    @Test(groups = {"regression", "api", "auth"}, description = "Login with blank fields -> 400")
    public void blankLoginFieldsReturn400() {
        Response response = api.login("", "");

        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("errors.email"), "Email is required");
        assertEquals(response.jsonPath().getString("errors.password"), "Password is required");
    }
}
