package com.voyanta.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Static utility class for one-way hashing. It creates a Base64-encoded SHA-256 hash
 * and is used to hash the client IP address, so anonymous rate-limit keys
 * do not store the raw IP.
 */
public final class HashUtil {

    private HashUtil() {}

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 mövcud deyil", e);
        }
    }
}