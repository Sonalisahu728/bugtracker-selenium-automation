package com.bugtracker.automation.ui;

import com.bugtracker.automation.base.BaseUiTest;
import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import com.bugtracker.automation.pages.DashboardPage;
import com.bugtracker.automation.utils.BrowserSession;
import com.bugtracker.automation.utils.TestDataFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ProjectTest extends BaseUiTest {

    @DataProvider(name = "newProjectButtonByRole")
    public Object[][] newProjectButtonByRole() {
        return new Object[][]{
                {Role.DEVELOPER, false},
                {Role.TEAM_LEAD, true},
                {Role.ADMIN, true}
        };
    }

    @Test(dataProvider = "newProjectButtonByRole", groups = {"regression", "rbac"},
            description = "Only Team Lead and Admin see the '+ New Project' button")
    public void newProjectButtonDependsOnRole(Role role, boolean expectedVisible) {
        TestUser user = api.registerUser(role);
        BrowserSession.loginWithToken(driver, user);

        DashboardPage dashboard = new DashboardPage(driver).open();

        assertEquals(dashboard.isNewProjectButtonVisible(), expectedVisible);
    }

    @Test(groups = {"smoke", "projects"}, description = "Team Lead creates a project from the UI")
    public void teamLeadCanCreateProject() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        BrowserSession.loginWithToken(driver, lead);
        String projectName = TestDataFactory.projectName();

        DashboardPage dashboard = new DashboardPage(driver).open()
                .createProject(projectName, "Created through the UI", null);

        assertTrue(dashboard.hasProject(projectName), "new project card should appear");
        assertTrue(dashboard.getProjectCardText(projectName).contains("Created by: " + lead.getName()));
    }

    @Test(groups = {"regression", "projects"}, description = "Invited member emails are added to the project")
    public void invitedMembersAreCounted() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        BrowserSession.loginWithToken(driver, lead);
        String projectName = TestDataFactory.projectName();

        DashboardPage dashboard = new DashboardPage(driver).open()
                .createProject(projectName, "With a member", developer.getEmail());

        assertTrue(dashboard.hasProject(projectName));
        assertTrue(dashboard.getProjectCardText(projectName).contains("2 members"),
                "creator + invited developer = 2 members");
    }

    @Test(groups = {"regression", "projects"}, description = "Project name is mandatory")
    public void projectNameIsRequired() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        BrowserSession.loginWithToken(driver, lead);

        DashboardPage dashboard = new DashboardPage(driver).open().submitEmptyProjectForm();

        assertFalse(dashboard.isProjectNameFieldValid());
        assertTrue(dashboard.isCreateProjectModalOpen(), "modal must stay open");
    }

    @Test(groups = {"regression", "projects", "rbac"},
            description = "Developers can see projects created by a Team Lead and open the board")
    public void developerSeesTeamLeadProject() {
        TestUser lead = api.registerUser(Role.TEAM_LEAD);
        TestUser developer = api.registerUser(Role.DEVELOPER);
        String projectName = TestDataFactory.projectName();
        api.createProjectAndGetId(lead, projectName);
        BrowserSession.loginWithToken(driver, developer);

        DashboardPage dashboard = new DashboardPage(driver).open();

        assertTrue(dashboard.hasProject(projectName));
        assertEquals(dashboard.openProject(projectName).getProjectName(), projectName);
    }
}
