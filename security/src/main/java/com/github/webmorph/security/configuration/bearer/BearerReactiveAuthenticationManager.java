package com.github.webmorph.security.configuration.bearer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.webmorph.security.account.event.AccountAuthenticateEvent;
import com.github.webmorph.security.account.model.Account;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

/**
 * Reactive authentication manager that validates {@link BearerAuthenticationToken} instances
 * using the configured JWT {@link Algorithm}.
 * <p>
 * Supports on-the-fly JWT validation and token generation for {@link Account}-based users.
 * If no algorithm is provided in the application context, a fallback dummy algorithm is used.
 * <p>
 * This component is central to JWT security handling in a reactive RSocket or WebFlux context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BearerReactiveAuthenticationManager implements ReactiveAuthenticationManager {
    private final Optional<Algorithm> algorithm;
    /**
     * Dummy fallback algorithm used if no real {@link Algorithm} bean is defined.
     * <p>
     * Intended for development or non-secure environments.
     */
    @Getter
    private final Algorithm dummyAlgorithm = Algorithm.HMAC256("dummy");

    /**
     * Logs a warning if no authentication algorithm is configured.
     * Invoked automatically after construction.
     */
    @PostConstruct
    public void warnOnDummyAlgorithm() {
        if (this.algorithm.isEmpty()) log.warn("""
                No authentication algorithm configured. Algorithm bean not found in current application context.""");
    }

    /**
     * Attempts to authenticate a given {@link Authentication} object reactively.
     * <p>
     * If the authentication is an instance of {@link BearerAuthenticationToken},
     * it is validated via JWT signature and claims. If validation succeeds,
     * the token is marked as authenticated.
     *
     * @param authentication the authentication request object
     * @return a {@link Mono} emitting the authenticated token or empty if not supported
     */
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return this.validate(authentication)
                .doOnNext(authentication::setAuthenticated)
                .thenReturn(authentication);
    }

    /**
     * Validates the JWT contained in a {@link BearerAuthenticationToken}.
     * <p>
     * The token is verified using the configured or fallback algorithm and must match:
     * <ul>
     *     <li>{@code sub} — the user's UUID</li>
     *     <li>{@code ema} — the user's username/email</li>
     *     <li>{@code pwd} — the current bcrypt hash stored in the user's account metadata</li>
     * </ul>
     * <p>
     * This mechanism ensures that all previously issued tokens become invalid
     * if the user changes their password, without requiring token blacklists or storage.
     *
     * @param authentication the authentication token to validate
     * @return {@code true} if the token is valid and up-to-date; {@code false} otherwise
     */
    public Mono<Boolean> validate(Authentication authentication) {
        if (!(authentication instanceof BearerAuthenticationToken token)) return Mono.just(false);
        Account principal = token.getPrincipal();
        return Mono.fromCallable(() -> {
            JWT.require(this.algorithm.orElse(this.dummyAlgorithm))
                    .withSubject(principal.getUuid().toString())
                    .withClaim("ema", principal.getUsername())
                    .withClaim("pwd", principal.getMetadata("password"))
                    .build()
                    .verify(token.getCredentials());
            return true;
        }).onErrorReturn(false);
    }

    /**
     * Generates a signed JWT token for the given account.
     * <p>
     * The token includes the following claims:
     * <ul>
     *     <li>{@code sub} — the user's UUID</li>
     *     <li>{@code ema} — the username or email</li>
     *     <li>{@code pwd} — the bcrypt hash of the current password</li>
     * </ul>
     * <p>
     * The {@code pwd} claim stores the bcrypt hash of the current password.
     * It is used to automatically invalidate previously issued tokens
     * when the user changes their password. As soon as the password changes,
     * the hash in the account will differ from the one in old tokens, causing validation to fail.
     * <p>
     * The expiration time is determined by the {@code rememberMe} flag.
     *
     * @param account    the user account to issue the token for
     * @param rememberMe if {@code false}, the token will expire in 6 hours; otherwise, in 1 year
     * @return a {@link Mono} containing the signed JWT token
     */

    public Mono<String> generateJWT(Account account, boolean rememberMe) {
        Instant now = Instant.now();
        return Mono.fromCallable(() -> JWT.create()
                .withSubject(account.getUuid().toString())
                .withClaim("ema", account.getUsername())
                .withClaim("pwd", account.getMetadata("password"))
                .withExpiresAt(now.plus(new AccountAuthenticateEvent(account.getUsername(), rememberMe)
                        .<AccountAuthenticateEvent>dispatch().getLifetime()))
                .sign(this.algorithm.orElse(this.dummyAlgorithm)));
    }
}
