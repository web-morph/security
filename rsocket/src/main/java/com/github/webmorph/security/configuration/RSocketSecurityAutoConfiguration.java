package com.github.webmorph.security.configuration;

import com.github.webmorph.security.configuration.bearer.BearerReactiveAuthenticationManager;
import com.github.webmorph.security.configuration.bearer.PayloadExchangeBearerConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.PayloadInterceptorOrder;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.rsocket.authentication.AuthenticationPayloadInterceptor;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;

/**
 * Core RSocket security configuration.
 * <p>
 * Enables reactive method security and configures authentication and metadata-based
 * authorization for RSocket payloads using a custom JWT-based flow.
 */
@AutoConfiguration
@EnableRSocketSecurity
@EnableReactiveMethodSecurity
public class RSocketSecurityAutoConfiguration {
    /**
     * Configures the {@link AuthenticationPayloadInterceptor} for RSocket authentication.
     * <p>
     * Uses {@link BearerReactiveAuthenticationManager} to validate tokens and
     * {@link PayloadExchangeBearerConverter} to extract credentials from metadata.
     *
     * @param authenticationManager the reactive authentication manager
     * @param converter             the payload-to-authentication converter
     * @return the configured payload interceptor
     */
    @Bean
    public AuthenticationPayloadInterceptor payloadInterceptor(BearerReactiveAuthenticationManager authenticationManager, PayloadExchangeBearerConverter converter) {
        AuthenticationPayloadInterceptor authInterceptor = new AuthenticationPayloadInterceptor(authenticationManager);
        authInterceptor.setAuthenticationConverter(converter);
        authInterceptor.setOrder(PayloadInterceptorOrder.AUTHENTICATION.getOrder());
        return authInterceptor;
    }

    /**
     * Registers the RSocket security interceptor chain.
     * <p>
     * This example permits all payloads globally and assumes authorization is handled
     * at the method level via {@code @PreAuthorize}, {@code @PostAuthorize}, etc.
     *
     * @param rsocket     the RSocket security builder
     * @param interceptor the authentication payload interceptor
     * @return the final {@link PayloadSocketAcceptorInterceptor} for connection handling
     */
    @Bean
    public PayloadSocketAcceptorInterceptor authorizationToken(RSocketSecurity rsocket, AuthenticationPayloadInterceptor interceptor) {
        return rsocket
                .addPayloadInterceptor(interceptor)
                .authorizePayload(authorize -> authorize.anyExchange().permitAll())
                .build();
    }
}
