package com.github.webmorph.security.account.exception;

/**
 * Thrown to indicate that authentication has failed due to invalid credentials.
 * <p>
 * Typically raised when a provided username or password is incorrect
 * during login or token generation.
 */
public class BadCredentialsException extends RuntimeException {
    public static final BadCredentialsException INVALID_CREDENTIALS = new BadCredentialsException("Invalid username or password.");
    public static final BadCredentialsException EXPIRED_OR_INVALID_TOKEN = new BadCredentialsException("Token expired or invalid.");

    protected BadCredentialsException(String message) {
        super(message);
    }
}
