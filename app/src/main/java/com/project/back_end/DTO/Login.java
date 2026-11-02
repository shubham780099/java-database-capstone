package com.project.back_end.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Login request body used by doctors and patients ({@code {"email": "...", "password": "..."}}).
 * Admins log in with an {@code Admin} object (username + password) instead.
 */
public class Login {

    // "identifier" is accepted as an alias so clients written against the generic
    // identifier-based login payload keep working.
    @JsonAlias("identifier")
    private String email;

    private String password;

    public Login() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
