package com.bugtracker.automation.pages;

import com.bugtracker.automation.model.Role;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class RegisterPage extends BasePage {

    private static final By NAME = By.id("name");
    private static final By EMAIL = By.id("email");
    private static final By PASSWORD = By.id("password");
    private static final By ROLE = By.id("role");
    private static final By SUBMIT = By.cssSelector("#registerForm button[type='submit']");
    private static final By ERROR_BOX = By.id("errorBox");

    public RegisterPage(WebDriver driver) {
        super(driver);
    }

    public RegisterPage open() {
        driver.get(baseUrl() + "/register.html");
        visible(SUBMIT);
        return this;
    }

    public RegisterPage fillForm(String name, String email, String password, Role role) {
        type(NAME, name);
        type(EMAIL, email);
        type(PASSWORD, password);
        selectByValue(ROLE, role.name());
        return this;
    }

    public RegisterPage submit() {
        click(SUBMIT);
        return this;
    }

    public DashboardPage registerAs(String name, String email, String password, Role role) {
        fillForm(name, email, password, role).submit();
        return new DashboardPage(driver).waitUntilLoaded();
    }

    public String getErrorMessage() {
        return waitForErrorBox(ERROR_BOX);
    }

    public boolean isEmailFieldValid() {
        return isFieldValid(EMAIL);
    }

    public boolean isPasswordFieldValid() {
        return isFieldValid(PASSWORD);
    }
}
