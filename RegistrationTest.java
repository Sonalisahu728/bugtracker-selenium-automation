package com.bugtracker.automation.ui;

import com.bugtracker.automation.base.BaseUiTest;
import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import com.bugtracker.automation.pages.DashboardPage;
import com.bugtracker.automation.pages.RegisterPage;
import com.bugtracker.automation.utils.TestDataFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RegistrationTest extends BaseUiTest {

    @DataProvider(name = "roles")
    public Object[][] roles() {
        return new Object[][]{{Role.DEVELOPER}, {Role.TEAM_LEAD}, {Role.ADMIN}};
    }

    @Test(dataProvider = "roles", groups = {"smoke", "auth"},
            description = "Each role can sign up and is shown on the dashboard with that role")
    public void registerEachRole(Role role) {
        String name = TestDataFactory.personName();

        DashboardPage dashboard = new RegisterPage(driver).open()
                .registerAs(name, TestDataFactory.uniqueEmail(), TestUser.DEFAULT_PASSWORD, role);

        assertEquals(dashboard.getWhoAmI(), name + " · " + role.name());
    }

    @Test(groups = {"regression", "auth"}, description = "Registering an email that already exists shows an error")
    public void duplicateEmailIsRejected() {
        TestUser existing = api.registerUser(Role.DEVELOPER);

        RegisterPage page = new RegisterPage(driver).open()
                .fillForm(TestDataFactory.personName(), existing.getEmail(), TestUser.DEFAULT_PASSWORD, Role.DEVELOPER)
                .submit();

        assertEquals(page.getErrorMessage(), "Email already registered");
        assertTrue(page.currentUrl().contains("register.html"));
    }

    @Test(groups = {"regression", "auth"}, description = "Password shorter than 6 characters is blocked (boundary: 5)")
    public void shortPasswordIsBlocked() {
        RegisterPage page = new RegisterPage(driver).open()
                .fillForm(TestDataFactory.personName(), TestDataFactory.uniqueEmail(), "12345", Role.DEVELOPER)
                .submit();

        assertFalse(page.isPasswordFieldValid(), "5 characters is below the minimum of 6");
        assertTrue(page.currentUrl().contains("register.html"));
    }

    @Test(groups = {"regression", "auth"}, description = "Password of exactly 6 characters is accepted (boundary: 6)")
    public void sixCharacterPasswordIsAccepted() {
        DashboardPage dashboard = new RegisterPage(driver).open()
                .registerAs(TestDataFactory.personName(), TestDataFactory.uniqueEmail(), "123456", Role.DEVELOPER);

        assertTrue(dashboard.currentUrl().contains("dashboard.html"));
    }

    @Test(groups = {"regression", "auth"}, description = "Malformed email address is blocked")
    public void invalidEmailFormatIsBlocked() {
        RegisterPage page = new RegisterPage(driver).open()
                .fillForm(TestDataFactory.personName(), "not-an-email", TestUser.DEFAULT_PASSWORD, Role.DEVELOPER)
                .submit();

        assertFalse(page.isEmailFieldValid());
        assertTrue(page.currentUrl().contains("register.html"));
    }
}
