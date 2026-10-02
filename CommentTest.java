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
import static org.testng.Assert.assertTrue;

public class CommentTest extends BaseUiTest {

    @Test(groups = {"smoke", "comments"}, description = "A developer posts a comment and sees it with their name")
    public void developerCanPostComment() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName(), developer);
        String title = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, title, "BUG", "P1");
        BrowserSession.loginWithToken(driver, developer);
        String comment = TestDataFactory.commentText();

        BoardPage board = new BoardPage(driver).open(projectId).openIssue(title).postComment(comment);

        assertTrue(board.getCommentTexts().contains(comment));
        assertTrue(board.getCommentsAreaText().contains(developer.getName()), "author name is shown");
    }

    @Test(groups = {"regression", "comments"}, description = "An issue with no comments shows the empty-state message")
    public void emptyStateIsShown() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        String title = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, title, "TASK", "P3");
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId).openIssue(title);

        assertTrue(board.getCommentsAreaText().contains("No comments yet."));
    }

    @Test(groups = {"regression", "comments"}, description = "Whitespace-only comment is not posted")
    public void blankCommentIsIgnored() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        String title = TestDataFactory.issueTitle();
        api.createIssueAndGetId(lead, projectId, title, "TASK", "P3");
        BrowserSession.loginWithToken(driver, lead);

        BoardPage board = new BoardPage(driver).open(projectId).openIssue(title).clickPostCommentWithEmptyText();

        assertTrue(board.getCommentTexts().isEmpty());
        assertTrue(board.getCommentsAreaText().contains("No comments yet."));
    }

    @Test(groups = {"regression", "comments"}, description = "Comments are listed oldest first")
    public void commentsAreOrderedOldestFirst() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        long projectId = api.createProjectAndGetId(lead, TestDataFactory.projectName());
        String title = TestDataFactory.issueTitle();
        long issueId = api.createIssueAndGetId(lead, projectId, title, "BUG", "P2");
        String first = TestDataFactory.commentText();
        String second = TestDataFactory.commentText();
        api.addComment(lead.getToken(), issueId, first);
        api.addComment(lead.getToken(), issueId, second);
        BrowserSession.loginWithToken(driver, lead);

        List<String> comments = new BoardPage(driver).open(projectId).openIssue(title).getCommentTexts();

        assertEquals(comments, List.of(first, second));
    }
}
