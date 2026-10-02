package com.bugtracker.automation.model;

/** A user created through the API for a test. Holds the JWT so tests can log in without the UI. */
public class TestUser {

    public static final String DEFAULT_PASSWORD = "Passw0rd!";

    private final String name;
    private final String email;
    private final String password;
    private final Role role;
    private long id;
    private String token;

    public TestUser(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public long getId() { return id; }
    public String getToken() { return token; }

    public void setId(long id) { this.id = id; }
    public void setToken(String token) { this.token = token; }
}
