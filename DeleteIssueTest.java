package com.bugtracker.automation.ui;

import com.bugtracker.automation.base.BaseUiTest;
import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import com.bugtracker.automation.pages.BoardPage;
import com.bugtracker.automation.utils.BrowserSession;
import com.bugtracker.automation.utils.TestDataFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** Rule: only Team Lead / Admin can delete, and only issues that are already CLOSED. */
public class DeleteIssueTest extends BaseUiTest {

    @Test(groups = {"regression", "issues"}, description = "Delete is not offered for an issue that is still open")
    public void deleteButtonHiddenForOpenIssue() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        String title = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, title, "BUG", "P2");
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId).openIssue(title);

        assertFalse(board.isDeleteButtonVisible());
    }

    @Test(groups = {"smoke", "issues"}, description = "Team Lead deletes a closed issue after confirming the dialog")
    public void teamLeadCanDeleteClosedIssue() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        String title = TestDataFactory.issueTitle();
        long issueId = api.createIssueAndGetId(lead, projectId, title, "BUG", "P2");
        api.moveIssueTo(lead, issueId, "CLOSED");
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId).openIssue(title);
        assertTrue(board.isDeleteButtonVisible());

        board.deleteIssue();

        assertTrue(board.waitUntilIssueGone(title), "card should be removed from the board");
        assertTrue(board.waitForColumnCount("CLOSED", 0));
    }

    @Test(groups = {"regression", "issues", "rbac"}, description = "Developers never see the delete button, even on closed issues")
    public void developerCannotDeleteClosedIssue() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), developer);
        String title = TestDataFactory.issueTitle();
        long issueId = api.createIssueAndGetId(lead, projectId, title, "BUG", "P2");
        api.moveIssueTo(lead, issueId, "CLOSED");
        BrowserSession.loginWithToken(driver, developer);

        BoardPage board = new BoardPage(driver).open(projectId).openIssue(title);

        assertFalse(board.isDeleteButtonVisible());
    }
}
