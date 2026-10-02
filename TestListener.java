package com.bugtracker.automation.listeners;

import com.bugtracker.automation.driver.DriverFactory;
import com.bugtracker.automation.utils.ScreenshotUtil;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.file.Path;

/** Logs results and saves a screenshot of the browser whenever a UI test fails. */
public class TestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("START  " + name(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("PASS   " + name(result));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("SKIP   " + name(result));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("FAIL   " + name(result) + " -> " + result.getThrowable());
        if (DriverFactory.get() != null) {
            Path shot = ScreenshotUtil.capture(DriverFactory.get(), result.getMethod().getMethodName());
            if (shot != null) {
                System.out.println("       screenshot: " + shot);
            }
        }
    }

    private String name(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
    }
}
