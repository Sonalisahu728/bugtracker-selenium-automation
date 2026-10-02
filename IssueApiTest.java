package com.bugtracker.automation.api;

import com.bugtracker.automation.base.BaseApiTest;
import com.bugtracker.automation.base.TransitionData;
import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import com.bugtracker.automation.utils.TestDataFactory;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class IssueApiTest extends BaseApiTest {

    // ------------------------------------------------------------- create

    @Test(groups = {"smoke", "api", "issues"}, description = "New issue is OPEN, priority defaults to P2")
    public void newIssueDefaults() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        Map<String, Object> body = new HashMap<>();
        body.put("title", TestDataFactory.issueTitle());
        body.put("type", "BUG");
        body.put("projectId", projectId);

        Response response = api.createIssue(lead.getToken(), body);

        assertEquals(response.statusCode(), 201);
        assertEquals(response.jsonPath().getString("status"), "OPEN");
        assertEquals(response.jsonPath().getString("priority"), "P2");
        assertEquals(response.jsonPath().getString("reportedByName"), lead.getName());
    }

    @Test(groups = {"regression", "api", "issues"}, description = "Missing title and type are reported per field")
    public void missingTitleAndTypeReturn400() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", projectId);

        Response response = api.createIssue(lead.getToken(), body);

        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("errors.title"), "Title is required");
        assertEquals(response.jsonPath().getString("errors.type"), "Type is required");
    }

    @Test(groups = {"regression", "api", "issues"}, description = "Unknown project id -> 400")
    public void unknownProjectReturns400() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);

        Response response = api.createIssue(lead.getToken(), 999999999L, TestDataFactory.issueTitle(), "BUG", "P1");

        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("message"), "Project not found");
    }

    // ------------------------------------------------------------- status

    @Test(dataProvider = "validTransitions", dataProviderClass = TransitionData.class,
            groups = {"regression", "api", "kanban"}, description = "Every allowed transition returns 200")
    public void validTransitionReturns200(String from, String to) {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        long issueId = api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P1");
        api.moveIssueTo(lead, issueId, from);

        Response response = api.updateStatus(lead.getToken(), issueId, to);

        assertEquals(response.statusCode(), 200);
        assertEquals(response.jsonPath().getString("status"), to);
    }

    @Test(dataProvider = "invalidTransitions", dataProviderClass = TransitionData.class,
            groups = {"smoke", "api", "kanban"}, description = "Every illegal transition returns 409 with a clear message")
    public void invalidTransitionReturns409(String from, String to) {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        long issueId = api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P1");
        api.moveIssueTo(lead, issueId, from);

        Response response = api.updateStatus(lead.getToken(), issueId, to);

        assertEquals(response.statusCode(), 409);
        assertEquals(response.jsonPath().getString("message"),
                "You cannot move an issue from " + from + " to " + to + " directly.");
    }

    @Test(groups = {"regression", "api", "kanban"}, description = "Status update on a missing issue -> 400")
    public void statusUpdateOnMissingIssueReturns400() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);

        Response response = api.updateStatus(lead.getToken(), 999999999L, "IN_PROGRESS");

        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("message"), "Issue not found with id: 999999999");
    }

    // ----------------------------------------------------------- delete

    @Test(groups = {"regression", "api", "issues"}, description = "Open issue cannot be deleted -> 409")
    public void deleteOpenIssueReturns409() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        long issueId = api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P2");

        Response response = api.deleteIssue(lead.getToken(), issueId);

        assertEquals(response.statusCode(), 409);
        assertEquals(response.jsonPath().getString("message"), "Only closed issues can be deleted");
    }

    @Test(groups = {"regression", "api", "issues"}, description = "Closed issue is deleted (204) and its comments go with it")
    public void deleteClosedIssueReturns204() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        long issueId = api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P2");
        api.addComment(lead.getToken(), issueId, TestDataFactory.commentText());
        api.moveIssueTo(lead, issueId, "CLOSED");

        Response response = api.deleteIssue(lead.getToken(), issueId);

        assertEquals(response.statusCode(), 204);
        List<Integer> remaining = api.getIssues(lead.getToken(), projectId, null).jsonPath().getList("id");
        assertFalse(remaining.contains((int) issueId), "issue should no longer be listed");
    }

    // ----------------------------------------------------------- filters

    @Test(groups = {"regression", "api", "filters"}, description = "Priority filter returns only matching issues")
    public void filterByPriority() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P0");
        api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P3");

        Response response = api.getIssues(lead.getToken(), projectId, Map.of("priority", "P0"));

        assertEquals(response.statusCode(), 200);
        List<String> priorities = response.jsonPath().getList("priority");
        assertEquals(priorities.size(), 1);
        assertTrue(priorities.stream().allMatch("P0"::equals));
    }

    @Test(groups = {"regression", "api", "filters"}, description = "Combined type + priority filter")
    public void filterByTypeAndPriority() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P1");
        api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "TASK", "P1");
        api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P2");

        Response response = api.getIssues(lead.getToken(), projectId, Map.of("type", "BUG", "priority", "P1"));

        assertEquals(response.jsonPath().getList("id").size(), 1);
    }

    // ------------------------------------------------------------ assign

    @Test(groups = {"smoke", "api", "issues", "rbac"}, description = "Team Lead assigns an issue; response shows the assignee")
    public void teamLeadCanAssign() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), developer);
        long issueId = api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P1");

        Response response = api.assign(lead.getToken(), issueId, developer.getId());

        assertEquals(response.statusCode(), 200);
        assertEquals(response.jsonPath().getLong("assignedToUserId"), developer.getId());
        assertEquals(response.jsonPath().getString("assignedToName"), developer.getName());
    }

    @Test(groups = {"regression", "api", "issues"}, description = "Assigning to a user that does not exist -> 400")
    public void assignToUnknownUserReturns400() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        long issueId = api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P1");

        Response response = api.assign(lead.getToken(), issueId, 999999999L);

        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("message"), "Assignee not found");
    }

    // ---------------------------------------------------------- comments

    @Test(groups = {"regression", "api", "comments"}, description = "Comment is created (201) with its author")
    public void addCommentReturns201() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        long issueId = api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P1");
        String text = TestDataFactory.commentText();

        Response response = api.addComment(lead.getToken(), issueId, text);

        assertEquals(response.statusCode(), 201);
        assertEquals(response.jsonPath().getString("text"), text);
        assertEquals(response.jsonPath().getString("authorName"), lead.getName());
    }

    @Test(groups = {"regression", "api", "comments"}, description = "Blank comment -> 400")
    public void blankCommentReturns400() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        long issueId = api.createIssueAndGetId(lead, projectId, TestDataFactory.issueTitle(), "BUG", "P1");

        Response response = api.addComment(lead.getToken(), issueId, "   ");

        assertEquals(response.statusCode(), 400);
        assertEquals(response.jsonPath().getString("errors.text"), "Comment text is required");
    }

    // ------------------------------------------------- suspected defects

    @Test(groups = {"known-defect", "api", "issues"},
            description = "An invalid enum value should be a 400, not a 500 (no handler for unreadable request bodies)")
    public void unknownPriorityValueReturns400() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());

        Response response = api.createIssue(lead.getToken(), projectId, TestDataFactory.issueTitle(), "BUG", "P9");

        assertEquals(response.statusCode(), 400, "got: " + response.asString());
    }
}
