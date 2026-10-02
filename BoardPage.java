package com.bugtracker.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The Kanban board for one project: columns, issue cards, filters and the issue-detail modal. */
public class BoardPage extends BasePage {

    // header + filters
    private static final By PROJECT_NAME = By.id("projectName");
    private static final By NEW_ISSUE_BUTTON = By.id("newIssueBtn");
    private static final By FILTER_PRIORITY = By.id("filterPriority");
    private static final By FILTER_TYPE = By.id("filterType");

    // "Report an Issue" modal
    private static final By ISSUE_TITLE = By.id("iTitle");
    private static final By ISSUE_DESC = By.id("iDesc");
    private static final By ISSUE_TYPE = By.id("iType");
    private static final By ISSUE_PRIORITY = By.id("iPriority");
    private static final By ISSUE_SUBMIT = By.cssSelector("#issueForm button[type='submit']");
    private static final By NEW_ISSUE_OVERLAY = By.cssSelector("#newIssueOverlay.show");

    // issue detail modal
    private static final By DETAIL_OVERLAY = By.cssSelector("#detailOverlay.show");
    private static final By DETAIL_TITLE = By.id("detailTitle");
    private static final By ASSIGN_FIELD = By.id("assignField");
    private static final By DETAIL_ASSIGNEE = By.id("detailAssignee");
    private static final By COMMENTS_LIST = By.id("commentsList");
    private static final By COMMENT_ITEMS = By.cssSelector("#commentsList .comment > div");
    private static final By NEW_COMMENT = By.id("newComment");
    private static final By POST_COMMENT = By.id("postCommentBtn");
    private static final By DELETE_ISSUE = By.id("deleteIssueBtn");
    private static final By CLOSE_DETAIL = By.id("closeDetailModal");

    /** Simulates an HTML5 drag-and-drop. Selenium's Actions class cannot drive native DnD reliably. */
    private static final String DRAG_AND_DROP_JS =
            "const card = arguments[0], column = arguments[1];"
                    + "const dt = new DataTransfer();"
                    + "const fire = (el, type) => el.dispatchEvent("
                    + "  new DragEvent(type, {bubbles: true, cancelable: true, dataTransfer: dt}));"
                    + "fire(card, 'dragstart'); fire(column, 'dragover'); fire(column, 'drop'); fire(card, 'dragend');";

    private static final Map<String, String> COUNT_IDS = Map.of(
            "OPEN", "countOpen",
            "IN_PROGRESS", "countProgress",
            "IN_REVIEW", "countReview",
            "CLOSED", "countClosed");

    public BoardPage(WebDriver driver) {
        super(driver);
    }

    public BoardPage open(long projectId) {
        driver.get(baseUrl() + "/board.html?projectId=" + projectId);
        return waitUntilLoaded();
    }

    public BoardPage waitUntilLoaded() {
        wait.until(d -> d.getCurrentUrl().contains("board.html")
                && !d.findElement(PROJECT_NAME).getText().isBlank()
                && !d.findElement(PROJECT_NAME).getText().startsWith("Loading"));
        return this;
    }

    public String getProjectName() {
        return visible(PROJECT_NAME).getText();
    }

    // ------------------------------------------------------------ create issue

    public boolean isReportIssueButtonVisible() {
        return isDisplayed(NEW_ISSUE_BUTTON);
    }

    public BoardPage reportIssue(String title, String description, String type, String priority) {
        click(NEW_ISSUE_BUTTON);
        type(ISSUE_TITLE, title);
        type(ISSUE_DESC, description);
        selectByValue(ISSUE_TYPE, type);
        if (priority != null) {
            selectByValue(ISSUE_PRIORITY, priority);   // when null the form keeps its default (P2)
        }
        click(ISSUE_SUBMIT);
        waitForToast("Issue reported");
        return this;
    }

    public BoardPage submitEmptyIssueForm() {
        click(NEW_ISSUE_BUTTON);
        click(ISSUE_SUBMIT);
        return this;
    }

    public boolean isIssueTitleFieldValid() {
        return isFieldValid(ISSUE_TITLE);
    }

    public boolean isReportIssueModalOpen() {
        return isDisplayed(NEW_ISSUE_OVERLAY);
    }

    // ---------------------------------------------------------------- cards

    private By titleInColumn(String status, String title) {
        return By.xpath("//div[@id='col" + status + "']//div[@class='card-title'][normalize-space()='" + title + "']");
    }

    private By anyCard(String title) {
        return By.xpath("//div[@class='card']//div[@class='card-title'][normalize-space()='" + title + "']");
    }

    private By cardContainer(String title) {
        return By.xpath("//div[@class='card'][.//div[@class='card-title'][normalize-space()='" + title + "']]");
    }

    public boolean isIssueInColumn(String title, String status) {
        return appearsWithin(titleInColumn(status, title), 10);
    }

    public boolean isIssueOnBoard(String title) {
        return appearsWithin(anyCard(title), 10);
    }

    /** Waits until the issue card has left the board (e.g. after a filter or a delete). */
    public boolean waitUntilIssueGone(String title) {
        try {
            return wait.until(ExpectedConditions.invisibilityOfElementLocated(anyCard(title)));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Visible text of a card, e.g. "Title\nP1 BUG\n→ Jane Doe" or "...Unassigned". */
    public String getCardText(String title) {
        return visible(cardContainer(title)).getText();
    }

    public boolean waitForColumnCount(String status, int expected) {
        By counter = By.id(COUNT_IDS.get(status));
        try {
            return wait.until(d -> d.findElement(counter).getText().trim().equals(String.valueOf(expected)));
        } catch (TimeoutException e) {
            return false;
        }
    }

    public int getColumnCount(String status) {
        return Integer.parseInt(visible(By.id(COUNT_IDS.get(status))).getText().trim());
    }

    // ------------------------------------------------------------ drag & drop

    public BoardPage dragIssueToColumn(String title, String targetStatus) {
        WebElement card = visible(cardContainer(title));
        WebElement column = driver.findElement(By.id("col" + targetStatus));
        ((JavascriptExecutor) driver).executeScript(DRAG_AND_DROP_JS, card, column);
        return this;
    }

    /** The message shown in the toast when the backend rejects a status change. */
    public String waitForTransitionError() {
        return waitForToast("cannot move an issue");
    }

    // ---------------------------------------------------------------- filters

    public BoardPage filterByPriority(String priority) {
        selectByValue(FILTER_PRIORITY, priority);
        return this;
    }

    public BoardPage filterByType(String type) {
        selectByValue(FILTER_TYPE, type);
        return this;
    }

    // ----------------------------------------------------------- issue detail

    public BoardPage openIssue(String title) {
        click(cardContainer(title));
        visible(DETAIL_OVERLAY);
        return this;
    }

    public String getDetailTitle() {
        return visible(DETAIL_TITLE).getText();
    }

    public boolean isAssignFieldVisible() {
        return isDisplayed(ASSIGN_FIELD);
    }

    /** Option labels in the "Assign to" dropdown, e.g. "Jane Doe (DEVELOPER)". */
    public List<String> getAssignOptions() {
        visible(DETAIL_ASSIGNEE);
        List<String> labels = new ArrayList<>();
        for (WebElement option : new Select(driver.findElement(DETAIL_ASSIGNEE)).getOptions()) {
            labels.add(option.getText());
        }
        return labels;
    }

    public BoardPage assignTo(String optionLabel) {
        new Select(visible(DETAIL_ASSIGNEE)).selectByVisibleText(optionLabel);
        waitForToast("Issue assigned");
        return this;
    }

    public boolean isDeleteButtonVisible() {
        return isDisplayed(DELETE_ISSUE);
    }

    public BoardPage deleteIssue() {
        click(DELETE_ISSUE);
        wait.until(ExpectedConditions.alertIsPresent()).accept();   // the app uses a native confirm()
        waitForToast("Issue deleted");
        return this;
    }

    public BoardPage postComment(String text) {
        type(NEW_COMMENT, text);
        click(POST_COMMENT);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(COMMENTS_LIST, text));
        return this;
    }

    public BoardPage clickPostCommentWithEmptyText() {
        driver.findElement(NEW_COMMENT).clear();
        click(POST_COMMENT);
        return this;
    }

    public List<String> getCommentTexts() {
        visible(COMMENTS_LIST);
        List<String> texts = new ArrayList<>();
        for (WebElement item : driver.findElements(COMMENT_ITEMS)) {
            texts.add(item.getText());
        }
        return texts;
    }

    public String getCommentsAreaText() {
        return visible(COMMENTS_LIST).getText();
    }

    public BoardPage closeDetail() {
        click(CLOSE_DETAIL);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(DETAIL_OVERLAY));
        return this;
    }
}
