package com.bugtracker.automation.ui;

import com.bugtracker.automation.base.BaseUiTest;
import com.bugtracker.automation.config.ConfigReader;
import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import com.bugtracker.automation.pages.DashboardPage;
import com.bugtracker.automation.pages.LoginPage;
import com.bugtracker.automation.utils.BrowserSession;
import com.bugtracker.automation.utils.TestDataFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class LoginTest extends BaseUiTest {

    @Test(groups = {"smoke", "auth"}, description = "Registered user can log in and lands on the dashboard")
    public void validLoginShowsDashboard() {
        TestUser user = api.registerUser(Role.DEVELOPER);

        DashboardPage dashboard = new LoginPage(driver).open().loginAs(user.getEmail(), user.getPassword());

        assertTrue(dashboard.currentUrl().contains("dashboard.html"));
        assertEquals(dashboard.getWhoAmI(), user.getName() + " · DEVELOPER");
    }

    @Test(groups = {"regression", "auth"}, description = "Wrong password is rejected with a clear message")
    public void wrongPasswordShowsError() {
        TestUser user = api.registerUser(Role.DEVELOPER);

        LoginPage login = new LoginPage(driver).open().loginExpectingFailure(user.getEmail(), "WrongPass123");

        assertEquals(login.getErrorMessage(), "Invalid email or password");
        assertTrue(login.currentUrl().contains("index.html"), "must stay on the login page");
    }

    @Test(groups = {"regression", "auth"}, description = "Unknown email gets the same generic message (no user enumeration)")
    public void unknownEmailShowsSameError() {
        LoginPage login = new LoginPage(driver).open()
                .loginExpectingFailure(TestDataFactory.uniqueEmail(), "Whatever123");

        assertEquals(login.getErrorMessage(), "Invalid email or password");
    }

    @Test(groups = {"regression", "auth"}, description = "Empty form is stopped by browser validation")
    public void emptyFormIsBlocked() {
        LoginPage login = new LoginPage(driver).open().submit();

        assertFalse(login.isEmailFieldValid(), "email field is required");
        assertTrue(login.currentUrl().contains("index.html"));
    }

    @Test(groups = {"regression", "auth"}, description = "Logging out clears the JWT and returns to login")
    public void logoutClearsSession() {
        TestUser user = api.registerUser(Role.TEAM_LEAD);
        BrowserSession.loginWithToken(driver, user);

        LoginPage login = new DashboardPage(driver).open().logout();

        assertTrue(login.currentUrl().contains("index.html"));
        assertNull(BrowserSession.readLocalStorage(driver, "bt_token"), "token must be removed");
        assertNull(BrowserSession.readLocalStorage(driver, "bt_user"), "user info must be removed");
    }

    @Test(groups = {"smoke", "auth", "security"}, description = "Protected pages redirect to login when there is no token")
    public void dashboardRequiresLogin() {
        driver.get(ConfigReader.get("base.url") + "/dashboard.html");

        assertTrue(new LoginPage(driver).waitForUrlContaining("index.html"),
                "an anonymous visitor must be sent to the login page");
    }

    @Test(groups = {"regression", "auth"}, description = "A logged-in user opening the login page is sent straight to the dashboard")
    public void loggedInUserSkipsLoginPage() {
        TestUser user = api.registerUser(Role.DEVELOPER);
        BrowserSession.loginWithToken(driver, user);

        driver.get(ConfigReader.get("base.url") + "/index.html");

        assertTrue(new LoginPage(driver).waitForUrlContaining("dashboard.html"));
    }
}
