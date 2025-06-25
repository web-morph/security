package com.github.webmorph.security.configuration.bearer;

import com.auth0.jwt.JWT;
import com.github.webmorph.security.account.AccountService;
import com.github.webmorph.security.account.model.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * A {@link ServerAuthenticationConverter} implementation that extracts a JWT token
 * from an HTTP cookie named {@code auth_token}, decodes it, and resolves the associated
 * {@link Account} using the {@link AccountService}.
 * <p>
 * If the token is valid and the corresponding account exists, an {@link Authentication}
 * object (specifically a {@link BearerAuthenticationToken}) is created and returned.
 */
@Component
@RequiredArgsConstructor
public class WebFluxBearerConverter implements ServerAuthenticationConverter {
    private final AccountService accountService;

    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getCookies().getFirst("auth_token"))
                .map(httpCookie -> JWT.decode(httpCookie.getValue()))
                .flatMap(token -> this.accountService.findByUuid(UUID.fromString(token.getSubject()))
                        .map(account -> new BearerAuthenticationToken(account, token)));
    }
}
