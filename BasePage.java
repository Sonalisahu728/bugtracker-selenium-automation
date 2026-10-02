package com.bugtracker.automation.pages;

import com.bugtracker.automation.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/** Shared helpers. Every wait is explicit - no Thread.sleep and no implicit wait anywhere. */
public abstract class BasePage {

    protected static final By TOAST = By.cssSelector("#toast.show");
    protected static final By WHO_AM_I = By.id("whoami");
    protected static final By LOGOUT_BUTTON = By.id("logoutBtn");

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("explicit.wait")));
    }

    protected String baseUrl() {
        return ConfigReader.get("base.url");
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String text) {
        WebElement element = visible(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected void selectByValue(By locator, String value) {
        new Select(visible(locator)).selectByValue(value);
    }

    protected boolean isDisplayed(By locator) {
        List<WebElement> found = driver.findElements(locator);
        return !found.isEmpty() && found.get(0).isDisplayed();
    }

    /** True if the element becomes visible within the given number of seconds. */
    protected boolean appearsWithin(By locator, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Waits for the "toast" notification that contains the given text and returns its full text. */
    protected String waitForToast(String expectedText) {
        return wait.until(d -> {
            List<WebElement> toasts = d.findElements(TOAST);
            if (!toasts.isEmpty() && toasts.get(0).getText().contains(expectedText)) {
                return toasts.get(0).getText();
            }
            return null;
        });
    }

    /** HTML5 constraint validation (required, minlength, type=email) is done by the browser. */
    public boolean isFieldValid(By locator) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver)
                .executeScript("return arguments[0].checkValidity();", visible(locator)));
    }

    protected String waitForErrorBox(By errorBox) {
        wait.until(d -> {
            List<WebElement> boxes = d.findElements(errorBox);
            return !boxes.isEmpty() && boxes.get(0).getAttribute("class").contains("show");
        });
        return driver.findElement(errorBox).getText();
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public boolean waitForUrlContaining(String fragment) {
        try {
            wait.until(ExpectedConditions.urlContains(fragment));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
