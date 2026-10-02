package com.bugtracker.automation.base;

import org.testng.annotations.DataProvider;

/**
 * Single source of truth for the status-transition rules, shared by UI and API tests
 * (rules come from IssueService.isValidTransition in the backend).
 */
public final class TransitionData {

    private TransitionData() { }

    @DataProvider(name = "validTransitions")
    public static Object[][] validTransitions() {
        return new Object[][]{
                {"OPEN", "IN_PROGRESS"},
                {"IN_PROGRESS", "IN_REVIEW"},
                {"IN_PROGRESS", "OPEN"},
                {"IN_REVIEW", "CLOSED"},
                {"IN_REVIEW", "IN_PROGRESS"},
                {"CLOSED", "OPEN"}
        };
    }

    @DataProvider(name = "invalidTransitions")
    public static Object[][] invalidTransitions() {
        return new Object[][]{
                {"OPEN", "IN_REVIEW"},
                {"OPEN", "CLOSED"},
                {"IN_PROGRESS", "CLOSED"},
                {"IN_REVIEW", "OPEN"},
                {"CLOSED", "IN_PROGRESS"},
                {"CLOSED", "IN_REVIEW"}
        };
    }
}
