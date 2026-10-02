package com.bugtracker.automation.base;

import com.bugtracker.automation.api.ApiClient;
import com.bugtracker.automation.driver.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/** Every UI test gets a fresh browser (clean localStorage) and an API client for test setup. */
public abstract class BaseUiTest {

    protected WebDriver driver;
    protected final ApiClient api = new ApiClient();

    @BeforeMethod(alwaysRun = true)
    public void startBrowser() {
        driver = DriverFactory.init();
    }

    @AfterMethod(alwaysRun = true)
    public void stopBrowser() {
        DriverFactory.quit();
    }
}
