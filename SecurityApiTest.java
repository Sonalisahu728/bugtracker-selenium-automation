package com.bugtracker.automation.api;

import com.bugtracker.automation.base.BaseApiTest;
import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import com.bugtracker.automation.utils.TestDataFactory;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** JWT handling and role-based access control, tested at the API level (not only hidden buttons in the UI). */
public class SecurityApiTest extends BaseApiTest {

    private static void assertRejected(Response response) {
        assertTrue(List.of(401, 403).contains(response.statusCode()),
                "expected 401/403 but got " + response.statusCode() + ": " + response.asString());
    }

    @Test(groups = {"smoke", "api", "security"}, description = "No token -> request rejected")
    public void missingTokenIsRejected() {
        assertRejected(api.getAllProjects(null));
    }

    @Test(groups = {"regression", "api", "security"}, description = "Valid token is accepted")
    public void validTokenIsAccepted() {
        TestUser user = api.registerUser(Role.DEVELOPER);

        assertEquals(api.getAllProjects(user.getToken()).statusCode(), 200);
    }

    @Test(groups = {"regression", "api", "security"}, description = "Token with a modified signature is rejected")
    public void tamperedTokenIsRejected() {
        TestUser user = api.registerUser(Role.DEVELOPER);
        String[] parts = user.getToken().split("\\.");
        char first = parts[2].charAt(0);
        String tamperedSignature = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertRejected(api.getAllProjects(tampered));
    }

    @Test(groups = {"regression", "api", "security"}, description = "Garbage token is rejected")
    public void malformedTokenIsRejected() {
        assertRejected(api.getAllProjects("this.is.not-a-jwt"));
    }

    @Test(groups = {"smoke", "api", "security", "rbac"}, description = "Developer cannot create a project -> 403")
    public void developerCannotCreateProject() {
        TestUser developer = api.registerUser(Role.DEVELOPER);

        Response response = api.createProject(developer.getToken(), TestDataFactory.projectName(), "x", null);

        assertEquals(response.statusCode(), 403);
    }

    @Test(groups = {"regression", "api", "security", "rbac"}, description = "Team Lead can create a project -> 201")
    public void teamLeadCanCreateProject() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);

        Response response = api.createProject(lead.getToken(), TestDataFactory.projectName(), "x", null);

        assertEquals(response.statusCode(), 201);
    }

    @Test(groups = {"regression", "api", "security", "rbac"}, description = "Developer cannot assign issues -> 403")
    public void developerCannotAssignIssue() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), developer);
        long issueId = api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P2");

        Response response = api.assign(developer.getToken(), issueId, developer.getId());

        assertEquals(response.statusCode(), 403);
    }

    @Test(groups = {"regression", "api", "security", "rbac"}, description = "Developer cannot delete issues -> 403, even closed ones")
    public void developerCannotDeleteIssue() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), developer);
        long issueId = api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P2");
        api.moveIssueTo(lead, issueId, "CLOSED");

        assertEquals(api.deleteIssue(developer.getToken(), issueId).statusCode(), 403);
    }

    @Test(groups = {"regression", "api", "security", "rbac"}, description = "Only the project's own Team Lead can add members")
    public void onlyOwnerCanAddMembers() {
        TestUser owner = api.registerUser(Role.TEAM_LEAD);
        TestUser otherLead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(owner, TestDataFactory.projectName());

        Response byOther = api.addMember(otherLead.getToken(), projectId, developer.getEmail());
        Response byOwner = api.addMember(owner.getToken(), projectId, developer.getEmail());

        assertEquals(byOther.statusCode(), 400);
        assertEquals(byOther.jsonPath().getString("message"), "Only the project's Team Lead can perform this action");
        assertEquals(byOwner.statusCode(), 200);
    }

    // ------------------------------------------------------------------
    // Suspected defects found by reading the backend source. They assert the CORRECT behaviour,
    // are excluded from the normal suites, and run on their own with testng-known-defects.xml.
    // ------------------------------------------------------------------

    @Test(groups = {"known-defect", "api", "security", "rbac"},
            description = "The UI says Developers cannot report issues, so the API should refuse them too (expected 403)")
    public void developerCannotCreateIssueViaApi() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), developer);

        Response response = api.createIssue(developer.getToken(), projectId, TestDataFactory.issueTitle(), "BUG", "P2");

        assertEquals(response.statusCode(), 403, "POST /api/issues is not role-restricted in SecurityConfig");
    }
}
