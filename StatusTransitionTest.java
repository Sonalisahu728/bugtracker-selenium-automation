package com.bugtracker.automation.ui;

import com.bugtracker.automation.base.BaseUiTest;
import com.bugtracker.automation.base.TransitionData;
import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import com.bugtracker.automation.pages.BoardPage;
import com.bugtracker.automation.utils.BrowserSession;
import com.bugtracker.automation.utils.TestDataFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * The Kanban drag-and-drop is the core feature, so its transition rules get a full matrix.
 * Allowed: OPEN->IN_PROGRESS, IN_PROGRESS->IN_REVIEW/OPEN, IN_REVIEW->CLOSED/IN_PROGRESS, CLOSED->OPEN.
 * Everything else must be rejected by the backend.
 */
public class StatusTransitionTest extends BaseUiTest {

    @Test(groups = {"smoke", "kanban"}, description = "Core flow: drag a new issue from Open to In Progress")
    public void cardCanBeDraggedToInProgress() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        String title = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, title, "BUG", "P1");
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId).dragIssueToColumn(title, "IN_PROGRESS");

        assertTrue(board.isIssueInColumn(title, "IN_PROGRESS"));
    }

    @Test(dataProvider = "validTransitions", dataProviderClass = TransitionData.class, groups = {"regression", "kanban"},
            description = "Dragging a card along an allowed path moves it to the new column")
    public void validDragMovesCard(String from, String to) {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        String title = TestDataFactory.issueTitle();
        long issueId = api.createIssueAndGetId(lead, projectId, title, "BUG", "P1");
        api.moveIssueTo(lead, issueId, from);
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId);
        assertTrue(board.isIssueInColumn(title, from), "precondition: card starts in " + from);

        board.dragIssueToColumn(title, to);

        assertTrue(board.isIssueInColumn(title, to), "card should now be in " + to);
        assertTrue(board.waitForColumnCount(to, 1), "counter of " + to + " column");
        assertTrue(board.waitForColumnCount(from, 0), "counter of " + from + " column");
    }

    @Test(dataProvider = "invalidTransitions", dataProviderClass = TransitionData.class, groups = {"regression", "kanban"},
            description = "Illegal drags are rejected server-side, shown as an error and the card does not move")
    public void invalidDragIsRejected(String from, String to) {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        String title = TestDataFactory.issueTitle();
        long issueId = api.createIssueAndGetId(lead, projectId, title, "BUG", "P1");
        api.moveIssueTo(lead, issueId, from);
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId);
        assertTrue(board.isIssueInColumn(title, from), "precondition: card starts in " + from);

        board.dragIssueToColumn(title, to);

        String toast = board.waitForTransitionError();
        assertTrue(toast.contains("from " + from + " to " + to), toast);
        assertTrue(board.isIssueInColumn(title, from), "card must stay in " + from);
    }

    @Test(groups = {"regression", "kanban"}, description = "A developer can also move cards (status changes are not role-restricted)")
    public void developerCanMoveCard() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), developer);
        String title = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, title, "TASK", "P2");
        BrowserSession.loginWithToken(driver, developer);

        BoardPage board = new BoardPage(driver).open(projectId).dragIssueToColumn(title, "IN_PROGRESS");

        assertTrue(board.isIssueInColumn(title, "IN_PROGRESS"));
    }
}
