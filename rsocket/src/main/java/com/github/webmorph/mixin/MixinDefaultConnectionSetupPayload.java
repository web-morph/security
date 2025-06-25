package com.github.webmorph.mixin;

import com.github.webmorph.security.configuration.rsocket.api.RSocketHolder;
import io.rsocket.RSocket;
import io.rsocket.core.DefaultConnectionSetupPayload;
import lombok.Getter;
import lombok.Setter;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Adds support for holding a reference to the {@link RSocket} that originated the payload exchange.
 * <p>
 * Implements the {@link RSocketHolder} interface to store and expose the {@link RSocket}
 * injected during interceptor pipeline execution.
 * <p>
 * This mixin allows external components to retrieve the {@link RSocket}
 * directly from {@link org.springframework.security.rsocket.core.DefaultPayloadExchange}.
 * <p>
 * Part.2 of closing FIX-ME in Spring Security RSocket: "do we want to make the sendingSocket available in the PayloadExchange".
 */
@Getter
@Setter
@Mixin(DefaultConnectionSetupPayload.class)
public class MixinDefaultConnectionSetupPayload implements RSocketHolder {
    private RSocket rSocket;
}
