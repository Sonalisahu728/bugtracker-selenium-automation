package com.bugtracker.automation.api;

import com.bugtracker.automation.config.ConfigReader;
import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import com.bugtracker.automation.utils.TestDataFactory;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Thin wrapper over the BugTrack REST API (REST Assured).
 * Methods that return {@link Response} are used by the API tests to assert status codes and bodies.
 * The "...AndGetId" / "registerUser" helpers are used by UI tests for fast, reliable test setup.
 */
public class ApiClient {

    private final String baseUri = ConfigReader.get("api.url");

    private RequestSpecification spec(String token) {
        RequestSpecification spec = given()
                .baseUri(baseUri)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
        if (token != null) {
            spec.header("Authorization", "Bearer " + token);
        }
        return spec;
    }

    // ---------------------------------------------------------------- auth

    public Response register(String name, String email, String password, String role) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        body.put("role", role);
        return spec(null).body(body).post("/auth/register");
    }

    public Response login(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        return spec(null).body(body).post("/auth/login");
    }

    /** Registers a brand-new user and returns it with id and JWT filled in. */
    public TestUser registerUser(Role role) {
        TestUser user = TestDataFactory.newUser(role);
        Response response = register(user.getName(), user.getEmail(), user.getPassword(), role.name());
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Setup failed - register returned "
                    + response.statusCode() + ": " + response.asString());
        }
        user.setId(response.jsonPath().getLong("userId"));
        user.setToken(response.jsonPath().getString("token"));
        return user;
    }

    // ------------------------------------------------------------ projects

    public Response createProject(String token, String name, String description, List<String> memberEmails) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("memberEmails", memberEmails == null ? new ArrayList<String>() : memberEmails);
        return spec(token).body(body).post("/projects");
    }

    public long createProjectAndGetId(TestUser owner, String name, TestUser... members) {
        List<String> emails = new ArrayList<>();
        for (TestUser member : members) {
            emails.add(member.getEmail());
        }
        Response response = createProject(owner.getToken(), name, "Created by automation", emails);
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Setup failed - create project returned "
                    + response.statusCode() + ": " + response.asString());
        }
        return response.jsonPath().getLong("id");
    }

    public Response getAllProjects(String token) {
        return spec(token).get("/projects/all");
    }

    public Response addMember(String token, long projectId, String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        return spec(token).body(body).post("/projects/" + projectId + "/members");
    }

    // -------------------------------------------------------------- issues

    public Response createIssue(String token, Map<String, Object> body) {
        return spec(token).body(body).post("/issues");
    }

    public Response createIssue(String token, long projectId, String title, String type, String priority) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", "Created by automation");
        body.put("type", type);
        body.put("priority", priority);
        body.put("projectId", projectId);
        return createIssue(token, body);
    }

    public long createIssueAndGetId(TestUser reporter, long projectId, String title, String type, String priority) {
        Response response = createIssue(reporter.getToken(), projectId, title, type, priority);
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Setup failed - create issue returned "
                    + response.statusCode() + ": " + response.asString());
        }
        return response.jsonPath().getLong("id");
    }

    public Response getIssues(String token, long projectId, Map<String, ?> filters) {
        RequestSpecification spec = spec(token);
        if (filters != null) {
            spec.queryParams(filters);
        }
        return spec.get("/issues/project/" + projectId);
    }

    public Response updateStatus(String token, long issueId, String status) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        return spec(token).body(body).put("/issues/" + issueId + "/status");
    }

    public Response assign(String token, long issueId, long assigneeId) {
        Map<String, Object> body = new HashMap<>();
        body.put("assignedToUserId", assigneeId);
        return spec(token).body(body).put("/issues/" + issueId + "/assign");
    }

    public Response deleteIssue(String token, long issueId) {
        return spec(token).delete("/issues/" + issueId);
    }

    /**
     * Moves a freshly created issue forward to the requested status using only legal transitions
     * (OPEN -> IN_PROGRESS -> IN_REVIEW -> CLOSED). Used to arrange the starting state of a test.
     */
    public void moveIssueTo(TestUser actor, long issueId, String targetStatus) {
        String[] path = {"IN_PROGRESS", "IN_REVIEW", "CLOSED"};
        for (String step : path) {
            if (targetStatus.equals("OPEN")) {
                return;
            }
            Response response = updateStatus(actor.getToken(), issueId, step);
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Setup failed - could not move issue to "
                        + step + ": " + response.asString());
            }
            if (step.equals(targetStatus)) {
                return;
            }
        }
    }

    // ------------------------------------------------------------ comments

    public Response addComment(String token, long issueId, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        return spec(token).body(body).post("/issues/" + issueId + "/comments");
    }
}
