package com.github.webmorph.mixin;

import com.github.webmorph.security.configuration.api.AuthHolder;
import lombok.Setter;
import org.spongepowered.asm.mixin.Mixin;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Adds Spring Security authentication support to the {@code RSocketRequester} connection level.
 * <p>
 * Implements the {@link AuthHolder} interface
 * to associate a deferred {@link Authentication} instance
 * with the {@code RSocketRequester}.
 * <p>
 * Enables a one-time authentication model or dynamic authentication updates,
 * allowing credentials to be applied at any moment during the connection lifecycle.
 * This removes the need to resend authentication metadata with each individual payload,
 * significantly reducing traffic and improving performance.
 * <p>
 * The authentication is injected externally via {@link #setAuth(Supplier)},
 * and retrieved reactively via {@link #getAuth()}.
 * <p>
 * Actual resolution and conversion of bearer tokens into {@code Authentication}
 * is implemented in
 * {@link com.github.webmorph.security.configuration.bearer.PayloadExchangeBearerConverter},
 * which supports both initial authentication during connection setup
 * and subsequent updates at runtime.
 */

@Setter
@Mixin(targets = "io.rsocket.core.RSocketRequester")
public class MixinRSocketRequester implements AuthHolder {
    private Supplier<Mono<Authentication>> auth = Mono::empty;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Authentication> getAuth() {
        return this.auth.get();
    }
}
