package com.github.webmorph.security.account.exception;

/**
 * Thrown to indicate that authentication has failed due to invalid credentials.
 * <p>
 * Typically raised when a provided username or password is incorrect
 * during login or token generation.
 */
public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException() {
        super("Invalid username or password.");
    }
}
