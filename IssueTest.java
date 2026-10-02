package com.bugtracker.automation.ui;

import com.bugtracker.automation.base.BaseUiTest;
import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import com.bugtracker.automation.pages.BoardPage;
import com.bugtracker.automation.utils.BrowserSession;
import com.bugtracker.automation.utils.TestDataFactory;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class IssueTest extends BaseUiTest {

    @Test(groups = {"smoke", "issues"}, description = "Team Lead reports a bug and it appears in the Open column")
    public void teamLeadCanReportBug() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        BrowserSession.loginWithToken(driver, lead);
        String title = TestDataFactory.issueTitle();

        BoardPage board = new BoardPage(driver).open(projectId)
                .reportIssue(title, "Steps to reproduce...", "BUG", "P1");

        assertTrue(board.isIssueInColumn(title, "OPEN"));
        assertTrue(board.waitForColumnCount("OPEN", 1));
        String card = board.getCardText(title);
        assertTrue(card.contains("P1") && card.contains("BUG") && card.contains("Unassigned"), card);
    }

    @Test(groups = {"regression", "issues"}, description = "Priority defaults to P2 when the user does not change it")
    public void priorityDefaultsToP2() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        BrowserSession.loginWithToken(driver, lead);
        String title = TestDataFactory.issueTitle();

        BoardPage board = new BoardPage(driver).open(projectId).reportIssue(title, "", "TASK", null);

        assertTrue(board.isIssueInColumn(title, "OPEN"));
        assertTrue(board.getCardText(title).contains("P2"));
    }

    @Test(groups = {"regression", "issues"}, description = "Issue title is mandatory")
    public void titleIsRequired() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId).submitEmptyIssueForm();

        assertFalse(board.isIssueTitleFieldValid());
        assertTrue(board.isReportIssueModalOpen());
    }

    @Test(groups = {"regression", "issues", "rbac"}, description = "Developers do not get a 'Report Issue' button")
    public void developerCannotSeeReportIssueButton() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), developer);
        BrowserSession.loginWithToken(driver, developer);

        BoardPage board = new BoardPage(driver).open(projectId);

        assertFalse(board.isReportIssueButtonVisible());
    }

    @Test(groups = {"regression", "issues", "filters"}, description = "Priority filter shows only matching issues")
    public void filterByPriority() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        String critical = TestDataFactory.issueTitle();
        String low = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, critical, "BUG", "P0");
        api.createIssueAndGetId(lead, projectId, low, "BUG", "P3");
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId);
        assertTrue(board.isIssueOnBoard(critical) && board.isIssueOnBoard(low), "both visible before filtering");

        board.filterByPriority("P0");

        assertTrue(board.waitUntilIssueGone(low), "P3 issue should be hidden");
        assertTrue(board.isIssueOnBoard(critical));
    }

    @Test(groups = {"regression", "issues", "filters"}, description = "Type filter shows only matching issues")
    public void filterByType() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        String bug = TestDataFactory.issueTitle();
        String feature = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, bug, "BUG", "P2");
        api.createIssueAndGetId(lead, projectId, feature, "FEATURE", "P2");
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId);
        assertTrue(board.isIssueOnBoard(bug) && board.isIssueOnBoard(feature));

        board.filterByType("FEATURE");

        assertTrue(board.waitUntilIssueGone(bug), "BUG should be hidden");
        assertTrue(board.isIssueOnBoard(feature));
    }

    @Test(groups = {"smoke", "issues", "rbac"}, description = "Team Lead assigns an issue to a developer")
    public void teamLeadCanAssignToDeveloper() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), developer);
        String title = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, title, "BUG", "P1");
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId).openIssue(title)
                .assignTo(developer.getName() + " (DEVELOPER)")
                .closeDetail();

        assertTrue(board.isIssueOnBoard(title));
        assertTrue(board.getCardText(title).contains("→ " + developer.getName()));
    }

    @Test(groups = {"regression", "issues", "rbac"}, description = "Developers never see the 'Assign to' field")
    public void developerDoesNotSeeAssignField() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), developer);
        String title = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, title, "TASK", "P2");
        BrowserSession.loginWithToken(driver, developer);

        BoardPage board = new BoardPage(driver).open(projectId).openIssue(title);

        assertFalse(board.isAssignFieldVisible());
    }

    @Test(groups = {"regression", "issues", "rbac"}, description = "A Team Lead can assign only to developers, not to other leads")
    public void teamLeadAssignListHasOnlyDevelopers() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser otherLead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), otherLead, developer);
        String title = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, title, "BUG", "P2");
        BrowserSession.loginWithToken(driver, lead);

        List<String> options = new BoardPage(driver).open(projectId).openIssue(title).getAssignOptions();

        assertTrue(options.contains(developer.getName() + " (DEVELOPER)"));
        assertFalse(options.contains(otherLead.getName() + " (TEAM_LEAD)"), "lead must not be assignable by a lead");
    }

    @Test(groups = {"regression", "issues", "rbac"}, description = "An Admin can assign to both Team Leads and Developers")
    public void adminAssignListHasLeadsAndDevelopers() {
        TestUser admin = api.registerUser(Role.ADMIN);
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(admin, TestDataFactory.projectName(), lead, developer);
        String title = TestDataFactory.issueTitle();
        api.createIssueAndGetId(admin, projectId, title, "BUG", "P2");
        BrowserSession.loginWithToken(driver, admin);

        List<String> options = new BoardPage(driver).open(projectId).openIssue(title).getAssignOptions();

        assertTrue(options.contains(lead.getName() + " (TEAM_LEAD)"));
        assertTrue(options.contains(developer.getName() + " (DEVELOPER)"));
    }
}
