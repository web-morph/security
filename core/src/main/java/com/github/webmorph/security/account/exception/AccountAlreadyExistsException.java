package com.github.webmorph.security.account.exception;

/**
 * Thrown to indicate that an account creation attempt has failed
 * because a user with the given username already exists.
 * <p>
 * Typically raised during registration when {@code username} uniqueness is enforced.
 */
public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException() {
        super("Account with given username already exists.");
    }
}
