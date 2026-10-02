package com.bugtracker.automation.utils;

import com.bugtracker.automation.model.Role;
import com.bugtracker.automation.model.TestUser;
import net.datafaker.Faker;

import java.util.UUID;

/** Generates unique data so tests never depend on each other and are safe to run in parallel. */
public final class TestDataFactory {

    private static final Faker FAKER = new Faker();

    private TestDataFactory() { }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String uniqueEmail() {
        return "qa." + shortId() + "@bugtrack-test.com";
    }

    public static String personName() {
        // letters only, so the name is safe inside XPath and JSON
        return FAKER.name().firstName().replaceAll("[^A-Za-z]", "") + " " + shortId();
    }

    public static String projectName() {
        return "AUTO-Project-" + shortId();
    }

    public static String issueTitle() {
        return "AUTO-Issue-" + shortId();
    }

    public static String commentText() {
        return "Comment " + shortId() + ": " + FAKER.lorem().sentence(4);
    }

    public static TestUser newUser(Role role) {
        return new TestUser(personName(), uniqueEmail(), TestUser.DEFAULT_PASSWORD, role);
    }
}
