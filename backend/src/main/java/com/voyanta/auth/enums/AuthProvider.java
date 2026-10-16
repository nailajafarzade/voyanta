package com.voyanta.auth.enums;

/**
 * Enum that tells how a user account was created: LOCAL for email and password
 * registration, GOOGLE or APPLE for social sign-in. It is stored on the User entity
 * so the app knows whether a password hash is expected.
 */
public enum AuthProvider {
    LOCAL, GOOGLE, APPLE
}