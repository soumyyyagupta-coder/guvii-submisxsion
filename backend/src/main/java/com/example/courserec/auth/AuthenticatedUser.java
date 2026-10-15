package com.example.courserec.auth;

public record AuthenticatedUser(long id, String email, String name, String role) {

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
