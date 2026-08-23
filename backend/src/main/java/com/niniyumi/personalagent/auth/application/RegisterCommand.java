package com.niniyumi.personalagent.auth.application;

public record RegisterCommand(String username, String email, String password, String displayName) {
}
