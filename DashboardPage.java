package com.bugtracker.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class DashboardPage extends BasePage {

    private static final By NEW_PROJECT_BUTTON = By.id("newProjectBtn");
    private static final By PROJECT_NAME_INPUT = By.id("pName");
    private static final By PROJECT_DESC_INPUT = By.id("pDesc");
    private static final By PROJECT_MEMBERS_INPUT = By.id("pMembers");
    private static final By PROJECT_SUBMIT = By.cssSelector("#projectForm button[type='submit']");
    private static final By MODAL_OVERLAY = By.cssSelector("#modalOverlay.show");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public DashboardPage open() {
        driver.get(baseUrl() + "/dashboard.html");
        return waitUntilLoaded();
    }

    public DashboardPage waitUntilLoaded() {
        wait.until(d -> d.getCurrentUrl().contains("dashboard.html")
                && !d.findElement(WHO_AM_I).getText().isBlank());
        return this;
    }

    /** Text of the top bar, e.g. "Jane Doe · TEAM_LEAD". */
    public String getWhoAmI() {
        return visible(WHO_AM_I).getText();
    }

    public boolean isNewProjectButtonVisible() {
        return isDisplayed(NEW_PROJECT_BUTTON);
    }

    public DashboardPage createProject(String name, String description, String memberEmailsCsv) {
        click(NEW_PROJECT_BUTTON);
        type(PROJECT_NAME_INPUT, name);
        type(PROJECT_DESC_INPUT, description);
        if (memberEmailsCsv != null && !memberEmailsCsv.isBlank()) {
            type(PROJECT_MEMBERS_INPUT, memberEmailsCsv);
        }
        click(PROJECT_SUBMIT);
        waitForToast("Project created");
        return this;
    }

    /** Submits the New Project form without filling anything in (browser validation should block it). */
    public DashboardPage submitEmptyProjectForm() {
        click(NEW_PROJECT_BUTTON);
        click(PROJECT_SUBMIT);
        return this;
    }

    public boolean isProjectNameFieldValid() {
        return isFieldValid(PROJECT_NAME_INPUT);
    }

    public boolean isCreateProjectModalOpen() {
        return isDisplayed(MODAL_OVERLAY);
    }

    private By projectCard(String projectName) {
        return By.xpath("//div[@id='projectGrid']//a[contains(@class,'project-card')]"
                + "[.//h3[normalize-space()='" + projectName + "']]");
    }

    public boolean hasProject(String projectName) {
        return appearsWithin(projectCard(projectName), 10);
    }

    /** Full visible text of a project card (name, description, "Created by", member count). */
    public String getProjectCardText(String projectName) {
        return visible(projectCard(projectName)).getText();
    }

    public BoardPage openProject(String projectName) {
        click(projectCard(projectName));
        return new BoardPage(driver).waitUntilLoaded();
    }

    public int getProjectCount() {
        List<WebElement> cards = driver.findElements(By.cssSelector("#projectGrid .project-card"));
        return cards.size();
    }

    public LoginPage logout() {
        click(LOGOUT_BUTTON);
        wait.until(d -> d.getCurrentUrl().contains("index.html"));
        return new LoginPage(driver);
    }
}
