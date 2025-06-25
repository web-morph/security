package com.github.webmorph.security.configuration.rsocket.exception;

import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.web.method.ControllerAdviceBean;

/**
 * Adapter record that bridges a {@link ControllerAdviceBean} into a {@link MessagingAdviceBean}
 * for use in Spring Messaging infrastructure (such as RSocket).
 *
 * <p>Spring Messaging expects components that implement {@code MessagingAdviceBean} to provide
 * advice (like exception handling) for message-handling methods. However, traditional
 * {@link org.springframework.web.bind.annotation.ControllerAdvice} beans are represented as
 * {@link ControllerAdviceBean}, which are not directly compatible with the messaging layer.</p>
 *
 * <p>This record acts as a lightweight wrapper around a {@code ControllerAdviceBean},
 * delegating all required methods to it so that the same advice classes can be reused
 * in both HTTP and messaging contexts (e.g., WebSocket, RSocket).</p>
 *
 * <p>By registering instances of this class into the message handler (e.g., {@code RSocketMessageHandler}),
 * you enable global exception handling using annotated methods like {@code @MessageExceptionHandler}.</p>
 *
 * @param adviceBean the controller advice bean to delegate to
 *
 * @see MessagingAdviceBean
 * @see org.springframework.web.bind.annotation.ControllerAdvice
// * @see org.springframework.web.bind.annotation.MessageExceptionHandler
 */
@SuppressWarnings("NullableProblems")
public record ExceptionMessageHandlerAdvice(ControllerAdviceBean adviceBean) implements MessagingAdviceBean {
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getBeanType() {
        return this.adviceBean.getBeanType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolveBean() {
        return this.adviceBean.resolveBean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicableToBeanType(final Class<?> beanType) {
        return this.adviceBean.isApplicableToBeanType(beanType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return this.adviceBean.getOrder();
    }
}
