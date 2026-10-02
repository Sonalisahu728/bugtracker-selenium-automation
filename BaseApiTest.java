package com.bugtracker.automation.base;

import com.bugtracker.automation.api.ApiClient;

/** API tests need no browser. */
public abstract class BaseApiTest {

    protected final ApiClient api = new ApiClient();
}
