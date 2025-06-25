package com.github.webmorph.mixin;

import com.github.webmorph.security.configuration.rsocket.api.RSocketHolder;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import net.lenni0451.classtransform.InjectionCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.springframework.security.rsocket.api.PayloadExchangeType;
import org.springframework.security.rsocket.core.DefaultPayloadExchange;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;

/**
 * Injects the sending {@link RSocket} into the Reactor {@link reactor.util.context.Context}
 * after the {@code PayloadSocketAcceptor#accept} method returns.
 * <p>
 * This makes the sending socket available downstream in the reactive context,
 * allowing other components (like interceptors) to access it via context lookup.
 * <p>
 * Part.1 of closing FIX-ME in Spring Security RSocket: "do we want to make the sendingSocket available in the PayloadExchange".
 */
@Mixin(targets = "org.springframework.security.rsocket.core.PayloadSocketAcceptor")
public class MixinPayloadSocketAcceptor {
    @Inject(method = "accept", at = @At(value = "RETURN"), cancellable = true)
    public void accept(ConnectionSetupPayload setup, RSocket sendingSocket, InjectionCallback callback) {
        ((RSocketHolder) setup).setRSocket(sendingSocket);
        Mono<RSocket> mono = callback.castReturnValue();
        callback.setReturnValue(mono != null ? mono.contextWrite(context -> context.put("rsocket", sendingSocket)) : Mono.empty());
    }

    @Redirect(method = "lambda$intercept$2", at = @At(
            value = "NEW",
            target = "Lorg/springframework/security/rsocket/core/DefaultPayloadExchange;<init>(Lorg/springframework/security/rsocket/api/PayloadExchangeType;Lio/rsocket/Payload;Lorg/springframework/util/MimeType;Lorg/springframework/util/MimeType;)V"
    ))
    private DefaultPayloadExchange extendDefaultPayloadExchange(PayloadExchangeType type, Payload payload, MimeType metadataMimeType,
                                                                MimeType dataMimeType) {
        DefaultPayloadExchange exchange = new DefaultPayloadExchange(type, payload, metadataMimeType, dataMimeType);
        ((RSocketHolder) exchange).setRSocket(((RSocketHolder) payload).getRSocket());
        return exchange;
    }
}
