package com.github.webmorph.security.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.webmorph.security.account.AccountService;
import com.github.webmorph.security.configuration.api.AuthHolder;
import com.github.webmorph.security.configuration.bearer.BearerAuthenticationToken;
import io.rsocket.RSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handles authentication requests for both RSocket and HTTP clients.
 * <p>
 * Provides a unified mechanism for generating authentication tokens,
 * supporting reactive session initiation over RSocket as well as traditional
 * cookie-based HTTP authentication.
 * <p>
 * Tokens are generated via the {@link AccountService} and may be sent
 * as bearer metadata over RSocket or as a secure cookie over HTTP.
 * <p>
 * This controller enforces anonymous-only access via {@code @PreAuthorize("isAnonymous()")},
 * preventing authenticated users from requesting new tokens.
 *
 * <p><strong>Endpoints:</strong></p>
 * <ul>
 *     <li>{@code POST /account/auth} — HTTP login with cookie-based token delivery</li>
 *     <li>{@code account.auth} (RSocket) — Reactive login with token returned in metadata</li>
 * </ul>
 *
 * @see AccountService
 * @see RSocketRequester
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RSocketAuthenticateController {
    private final AccountService accountService;

    /**
     * Internal method that generates a token using the {@link AccountService}.
     *
     * @param request the authentication request payload
     * @return a Mono emitting the {@link AuthenticateResponse} on success
     */
    public Mono<AuthenticateResponse> auth(AuthenticateRequest request) {
        return this.accountService.generateToken(request.username, request.password, request.rememberMe)
                .map(AuthenticateResponse::new);
    }


    /**
     * Handles RSocket-based authentication requests.
     * <p>
     * Generates a token and sends it back to the client via RSocket metadata
     * using the {@code message/x.rsocket.authentication.bearer.v0} MIME type.
     *
     * @param requester the RSocket requester used to send metadata back to the client
     * @param request   the authentication request payload
     * @return a Mono emitting the {@link AuthenticateResponse} after metadata is sent
     */
    @PreAuthorize("isAnonymous()")
    @MessageMapping("account.auth")
    @SuppressWarnings("ConstantConditions")
    public Mono<AuthenticateResponse> auth(RSocketRequester requester, @Payload AuthenticateRequest request) {
        return this.auth(request).doOnNext(response -> {
            DecodedJWT token = JWT.decode(response.token);
            RSocket rsocket = requester.rsocket();
            if (rsocket == null) return;
            ((AuthHolder) rsocket).setAuth(() -> this.accountService.findByUuid(UUID.fromString(token.getSubject()))
                    .map(account -> new BearerAuthenticationToken(account, token)));
        });
    }

    /**
     * Authentication response DTO.
     *
     * @param token the generated authentication token
     */
    public record AuthenticateResponse(String token) {

    }

    /**
     * Authentication request DTO.
     *
     * @param username   the username
     * @param password   the raw password
     * @param rememberMe whether to issue a long-lived token
     */
    public record AuthenticateRequest(String username, String password, boolean rememberMe) {

    }
}
