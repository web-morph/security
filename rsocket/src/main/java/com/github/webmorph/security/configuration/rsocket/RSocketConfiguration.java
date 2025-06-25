package com.github.webmorph.security.configuration.rsocket;

import com.github.webmorph.security.configuration.rsocket.exception.ExceptionMessageHandlerAdvice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.metadata.AuthMetadataCodec;
import io.rsocket.metadata.WellKnownAuthType;
import io.rsocket.metadata.WellKnownMimeType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver;
import org.springframework.util.MimeType;
import org.springframework.validation.Validator;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

import java.util.Optional;

/**
 * Spring configuration for customizing RSocket messaging, metadata extraction,
 * and exception handling for secure and structured communication.
 */
@Configuration
public class RSocketConfiguration {
    private static final MimeType MESSAGE_RSOCKET_AUTHENTICATION = MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.getString());

    /**
     * Configures {@link RSocketStrategies} with support for:
     * <ul>
     *     <li>JSON encoding/decoding via Jackson</li>
     *     <li>Route matching using {@link PathPatternRouteMatcher}</li>
     *     <li>Metadata extraction for RSocket authentication (Bearer/Simple)</li>
     * </ul>
     * <p>
     * This setup allows downstream access to token/credentials extracted from
     * {@code message/x.rsocket.authentication.v0} frames as key-value metadata.
     *
     * @return a configured {@link RSocketStrategies} instance
     */
    @Bean
    public RSocketStrategies getRSocketStrategies() {
        return RSocketStrategies.builder()
                .encoder(new Jackson2JsonEncoder())
                .decoder(new Jackson2JsonDecoder())
                .routeMatcher(new PathPatternRouteMatcher())
                .metadataExtractorRegistry(registry ->
                        registry.metadataToExtract(MESSAGE_RSOCKET_AUTHENTICATION, byte[].class, (s, map) -> {
                            ByteBuf raw = Unpooled.wrappedBuffer(s);
                            WellKnownAuthType value = AuthMetadataCodec.readWellKnownAuthType(raw);
                            switch (value) {
                                case BEARER -> {
                                    map.put("authType", WellKnownAuthType.BEARER);
                                    map.put("token", new String(AuthMetadataCodec.readBearerTokenAsCharArray(raw)));
                                }
                                case SIMPLE -> {
                                    map.put("authType", WellKnownAuthType.SIMPLE);
                                    map.put("username", new String(AuthMetadataCodec.readUsernameAsCharArray(raw)));
                                    map.put("password", new String(AuthMetadataCodec.readPasswordAsCharArray(raw)));
                                }
                            }
                        }))
                .build();
    }

    /**
     * Configures the {@link RSocketMessageHandler} to:
     * <ul>
     *     <li>Use the provided {@link RSocketStrategies}</li>
     *     <li>Support {@code @AuthenticationPrincipal} argument resolution</li>
     *     <li>Register controller-level exception handling via {@link ExceptionMessageHandlerAdvice}</li>
     * </ul>
     *
     * @param strategies the configured RSocket strategies
     * @param validator  the validator for input validation (e.g. {@code @Valid})
     * @param context    the Spring application context (used to discover controller advices)
     * @return a fully configured {@link RSocketMessageHandler} bean
     */
    @Bean
    public RSocketMessageHandler messageHandler(RSocketStrategies strategies, Optional<Validator> validator, ApplicationContext context) {
        RSocketMessageHandler messageHandler = new RSocketMessageHandler();
        messageHandler.getArgumentResolverConfigurer().addCustomResolver(new AuthenticationPrincipalArgumentResolver());
        messageHandler.setRSocketStrategies(strategies);
        validator.ifPresent(messageHandler::setValidator);
        ControllerAdviceBean.findAnnotatedBeans(context).forEach(bean ->
                messageHandler.registerMessagingAdvice(new ExceptionMessageHandlerAdvice(bean)));
        return messageHandler;
    }
}
