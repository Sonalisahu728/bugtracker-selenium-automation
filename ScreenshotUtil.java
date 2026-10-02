package com.bugtracker.automation.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class ScreenshotUtil {

    private static final Path OUTPUT_DIR = Paths.get("target", "screenshots");

    private ScreenshotUtil() { }

    /** Saves a PNG under target/screenshots and returns its path, or null if it could not be taken. */
    public static Path capture(WebDriver driver, String testName) {
        if (!(driver instanceof TakesScreenshot)) {
            return null;
        }
        try {
            Files.createDirectories(OUTPUT_DIR);
            Path source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
            Path target = OUTPUT_DIR.resolve(testName + "-" + System.currentTimeMillis() + ".png");
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not capture screenshot: " + e.getMessage());
            return null;
        }
    }
}
