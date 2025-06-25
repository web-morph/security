package com.github.webmorph.mixin;

import com.github.webmorph.security.configuration.rsocket.api.RSocketHolder;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.springframework.security.rsocket.api.PayloadExchangeType;
import org.springframework.security.rsocket.core.DefaultPayloadExchange;
import org.springframework.util.MimeType;
import reactor.util.context.Context;

/**
 * Redirects the instantiation of {@link DefaultPayloadExchange}
 * inside the interceptor pipeline to inject the {@link RSocket} from the Reactor {@link Context}.
 * <p>
 * Ensures that the {@link DefaultPayloadExchange} carries a reference to the {@link RSocket}
 * that initiated the connection, allowing access to connection-level metadata or state.
 * <p>
 * Requires that {@link DefaultPayloadExchange} implements {@link RSocketHolder}.
 * <p>
 * Part.3 (final) of closing FIX-ME in Spring Security RSocket: "do we want to make the sendingSocket available in the PayloadExchange".
 */
@Mixin(targets = "org.springframework.security.rsocket.core.PayloadInterceptorRSocket")
public class MixinPayloadInterceptorRSocket {
    @Shadow
    private Context context;

    @Redirect(method = "lambda$intercept$7", at = @At(
            value = "NEW",
            target = "Lorg/springframework/security/rsocket/core/DefaultPayloadExchange;<init>(Lorg/springframework/security/rsocket/api/PayloadExchangeType;Lio/rsocket/Payload;Lorg/springframework/util/MimeType;Lorg/springframework/util/MimeType;)V"
    ))
    private DefaultPayloadExchange extendDefaultPayloadExchange(PayloadExchangeType type, Payload payload, MimeType metadataMimeType,
                                                                MimeType dataMimeType) {
        DefaultPayloadExchange exchange = new DefaultPayloadExchange(type, payload, metadataMimeType, dataMimeType);
        ((RSocketHolder) exchange).setRSocket(this.context.get("rsocket"));
        return exchange;
    }
}
