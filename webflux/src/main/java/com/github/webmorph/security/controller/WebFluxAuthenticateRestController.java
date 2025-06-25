package com.github.webmorph.security.controller;

import com.github.webmorph.security.account.AccountService;
import com.github.webmorph.security.account.event.AccountAuthenticateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
 */
@RestController
@RequiredArgsConstructor
public class WebFluxAuthenticateRestController {
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
     * Handles HTTP-based authentication requests.
     * <p>
     * Generates a token and sets it as a secure cookie in the response.
     *
     * @param request the authentication request payload
     * @return a {@link ResponseEntity} with the token and set-cookie header
     */
    @PreAuthorize("isAnonymous()")
    @PostMapping("/account/auth")
    public Mono<ResponseEntity<AuthenticateResponse>> restAuth(@RequestBody AuthenticateRequest request) {
        return this.auth(request).map(response -> {
            ResponseCookie cookie = ResponseCookie.from("auth_token", response.token())
                    .httpOnly(false)
                    .secure(true)
                    .path("/")
                    .maxAge(new AccountAuthenticateEvent(request.username, request.rememberMe)
                            .<AccountAuthenticateEvent>dispatch().getLifetime())
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(response);
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
