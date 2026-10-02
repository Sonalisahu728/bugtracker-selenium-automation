package com.bugtracker.automation.utils;

import com.bugtracker.automation.config.ConfigReader;
import com.bugtracker.automation.model.TestUser;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Hybrid-login helper: the user is created and authenticated through the API, then the JWT is
 * placed in localStorage exactly the way js/api.js does (keys bt_token and bt_user).
 * UI tests that are not about the login screen skip it and run much faster.
 */
public final class BrowserSession {

    private BrowserSession() { }

    public static void loginWithToken(WebDriver driver, TestUser user) {
        // Open the app first so localStorage belongs to the right origin.
        driver.get(ConfigReader.get("base.url") + "/register.html");
        ((JavascriptExecutor) driver).executeScript(
                "localStorage.setItem('bt_token', arguments[0]);"
                        + "localStorage.setItem('bt_user', JSON.stringify("
                        + "{id: arguments[1], name: arguments[2], email: arguments[3], role: arguments[4]}));",
                user.getToken(), user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    public static Object readLocalStorage(WebDriver driver, String key) {
        return ((JavascriptExecutor) driver).executeScript("return localStorage.getItem(arguments[0]);", key);
    }
}
