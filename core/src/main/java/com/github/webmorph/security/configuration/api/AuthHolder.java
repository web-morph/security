package com.github.webmorph.security.configuration.api;

import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Interface for managing connection-level authentication in RSocket communication.
 * <p>
 * Used to associate a reactive {@link Authentication} source with a given RSocket channel
 * (e.g., {@code RSocketRequester}), allowing authentication to be applied once per connection
 * and updated at runtime if needed.
 */
public interface AuthHolder {
    /**
     * Retrieves the current authentication associated with this {@code RSocketRequester}.
     * <p>
     * The returned {@link Mono} originates from the supplier previously injected via {@link #setAuth(Supplier)}.
     * This method supports reactive and lazy evaluation, allowing the authentication
     * to be computed, refreshed, or invalidated dynamically based on runtime context.
     *
     * @return a {@link Mono} containing the {@link Authentication}, or empty if none is set
     */
    Mono<Authentication> getAuth();

    /**
     * Injects a deferred authentication supplier into the {@code RSocketRequester}.
     * <p>
     * This method allows associating a reactive source of {@link Authentication}
     * with the current RSocket connection. The supplied authentication can be resolved
     * either once (e.g., during setup) or dynamically updated at any time during the connection.
     * <p>
     * This enables connection-level authentication that avoids per-request credential transmission.
     *
     * @param auth a {@link Supplier} that returns a {@link Mono} of {@link Authentication}
     */
    void setAuth(Supplier<Mono<Authentication>> auth);
}
