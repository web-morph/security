package com.github.webmorph.security.configuration.bearer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.webmorph.security.account.AccountService;
import com.github.webmorph.security.configuration.api.AuthHolder;
import com.github.webmorph.security.configuration.rsocket.api.RSocketHolder;
import io.rsocket.metadata.WellKnownAuthType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.security.core.Authentication;
import org.springframework.security.rsocket.api.PayloadExchange;
import org.springframework.security.rsocket.authentication.AuthenticationPayloadExchangeConverter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Custom {@link AuthenticationPayloadExchangeConverter} implementation that extracts
 * bearer or simple authentication data from the RSocket {@link PayloadExchange}.
 * <p>
 * Supports one-time connection-level authentication by injecting an {@link Authentication}
 * object into an {@link AuthHolder}, which is held in the active {@link RSocketHolder}.
 * <p>
 * Handles both {@link WellKnownAuthType#BEARER} (via JWT) and {@link WellKnownAuthType#SIMPLE}
 * (via username/password) authentication types.
 * <p>
 * This allows clients to authenticate once per connection without embedding credentials
 * in every payload, reducing network overhead.
 */
@Component
@RequiredArgsConstructor
public class PayloadExchangeBearerConverter extends AuthenticationPayloadExchangeConverter {
    private final RSocketStrategies strategies;
    private final AccountService accountService;

    /**
     * Attempts to extract and resolve authentication from the given {@link PayloadExchange}.
     * <p>
     * If the payload contains RSocket authentication metadata, it will either:
     * <ul>
     *   <li>Decode and process a Bearer token (JWT)</li>
     *   <li>Process SIMPLE auth by verifying username/password and issuing a JWT</li>
     * </ul>
     * The resolved {@link Authentication} will be cached in the associated {@link AuthHolder},
     * allowing reuse across the connection lifecycle.
     *
     * @param exchange the RSocket payload exchange
     * @return a {@link Mono} emitting the resolved {@link Authentication}, or empty if not present
     */
    @Override
    public Mono<Authentication> convert(PayloadExchange exchange) {
        Map<String, Object> extract = this.strategies.metadataExtractor().extract(exchange.getPayload(), exchange.getMetadataMimeType());
        Object authType = extract.get("authType");
        AuthHolder authHolder = this.getAuthHolder(exchange);
        if (authType == WellKnownAuthType.SIMPLE) {
            String username = extract.get("username").toString().toLowerCase();
            String password = extract.get("password").toString();
            return this.simple(authHolder, username, password);
        } else if (authType == WellKnownAuthType.BEARER) {
            DecodedJWT decoded = JWT.decode(extract.get("token").toString());
            return this.bearer(authHolder, decoded);
        }

        // Fallback to existing cached auth if no new metadata is found
        return authHolder.getAuth();
    }

    /**
     * Processes SIMPLE (username/password) authentication by generating a JWT
     * and falling back to the bearer token path.
     *
     * @param holder   the target connection-bound {@link AuthHolder}
     * @param username the login username
     * @param password the login password
     * @return a {@link Mono} with the resolved {@link Authentication}
     */
    private Mono<Authentication> simple(AuthHolder holder, String username, String password) {
        return this.accountService.generateToken(username, password, true)
                .map(JWT::decode)
                .flatMap(token -> this.bearer(holder, token));
    }

    /**
     * Processes a bearer (JWT) token and sets the corresponding {@link Authentication}
     * into the provided {@link AuthHolder}, to be reused for the session.
     *
     * @param holder the target connection-bound {@link AuthHolder}
     * @param token  the decoded JWT
     * @return a {@link Mono} with the authenticated user
     */
    private Mono<Authentication> bearer(AuthHolder holder, DecodedJWT token) {
        holder.setAuth(() -> this.accountService.findByUuid(UUID.fromString(token.getSubject()))
                .map(account -> new BearerAuthenticationToken(account, token)));
        return holder.getAuth();
    }

    /**
     * Extracts the {@link AuthHolder} from the connection's {@link RSocketHolder},
     * enabling storage of connection-bound authentication.
     *
     * @param exchange the current {@link PayloadExchange}
     * @return the {@link AuthHolder} bound to the active connection
     */
    private AuthHolder getAuthHolder(PayloadExchange exchange) {
        return (AuthHolder) ((RSocketHolder) exchange).getRSocket();
    }
}
