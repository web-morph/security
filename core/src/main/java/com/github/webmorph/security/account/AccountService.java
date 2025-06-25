package com.github.webmorph.security.account;

import com.github.webmorph.security.account.exception.AccountAlreadyExistsException;
import com.github.webmorph.security.account.exception.BadCredentialsException;
import com.github.webmorph.security.account.model.Account;
import com.github.webmorph.security.account.repository.AccountRepository;
import com.github.webmorph.security.configuration.bearer.BearerReactiveAuthenticationManager;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;


/**
 * Reactive service responsible for managing {@link Account} entities
 * and handling authentication-related operations such as token generation and account registration.
 * <p>
 * Delegates all repository-level methods to {@link AccountRepository}, while providing
 * high-level operations for login and sign-up.
 */
@Service
@RequiredArgsConstructor
public class AccountService {
    @Delegate
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final BearerReactiveAuthenticationManager authenticationManager;

    /**
     * Attempts to authenticate a user by username and password.
     * <p>
     * If credentials are valid, a signed JWT token is generated and returned.
     * Otherwise, the resulting stream emits a {@link BadCredentialsException}.
     *
     * @param username   the login identifier
     * @param password   the raw password to validate
     * @param rememberMe whether to issue a longer-lived token
     * @return a {@link Mono} emitting a signed JWT token or error if authentication fails
     */
    public Mono<String> generateToken(String username, String password, boolean rememberMe) {
        return this.accountRepository.findByUsername(username)
                .flatMap(account -> {
                    if (!account.passwordMatches(password)) return Mono.empty();
                    return this.authenticationManager.generateJWT(account, rememberMe);
                })
                .switchIfEmpty(Mono.error(Throwable::new))
                .onErrorMap(throwable -> new BadCredentialsException());
    }

    /**
     * Registers a new account with the given username and password.
     * <p>
     * If the username already exists, an {@link AccountAlreadyExistsException} is thrown.
     * Otherwise, a new {@link Account} is created, its metadata is initialized,
     * and a JWT token is generated and returned.
     *
     * @param username the desired username (case-insensitive)
     * @param password the raw password to be hashed and stored
     * @return a {@link Mono} emitting a signed JWT token for the newly created account
     */
    public Mono<String> createAccount(String username, String password) {
        return this.accountRepository.existsByUsername(username)
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.error(new AccountAlreadyExistsException()))
                .switchIfEmpty(this.accountRepository.findOrCreate(UUID.randomUUID(), username.toLowerCase()))
                .cast(Account.class)
                .flatMap(account -> {
                    account.setMetadata("username", username.toLowerCase());
                    account.setMetadata("password", this.passwordEncoder.encode(password));
                    return account.save().then(Mono.defer(() -> this.authenticationManager.generateJWT(account, true)));
                });
    }
}
