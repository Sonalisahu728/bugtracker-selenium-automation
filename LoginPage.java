package com.bugtracker.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private static final By EMAIL = By.id("email");
    private static final By PASSWORD = By.id("password");
    private static final By SUBMIT = By.cssSelector("#loginForm button[type='submit']");
    private static final By ERROR_BOX = By.id("errorBox");
    private static final By REGISTER_LINK = By.linkText("Create an account");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        driver.get(baseUrl() + "/index.html");
        visible(SUBMIT);
        return this;
    }

    public LoginPage enterCredentials(String email, String password) {
        type(EMAIL, email);
        type(PASSWORD, password);
        return this;
    }

    public LoginPage submit() {
        click(SUBMIT);
        return this;
    }

    /** Use for a login that is expected to succeed. */
    public DashboardPage loginAs(String email, String password) {
        enterCredentials(email, password).submit();
        return new DashboardPage(driver).waitUntilLoaded();
    }

    /** Use for a login that is expected to fail - stays on this page. */
    public LoginPage loginExpectingFailure(String email, String password) {
        enterCredentials(email, password).submit();
        return this;
    }

    public String getErrorMessage() {
        return waitForErrorBox(ERROR_BOX);
    }

    public boolean isEmailFieldValid() {
        return isFieldValid(EMAIL);
    }

    public RegisterPage goToRegister() {
        click(REGISTER_LINK);
        return new RegisterPage(driver);
    }
}
