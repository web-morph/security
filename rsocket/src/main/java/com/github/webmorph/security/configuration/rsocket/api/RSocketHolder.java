package com.github.webmorph.security.configuration.rsocket.api;

import io.rsocket.RSocket;

/**
 * Interface for associating an {@link RSocket} instance with an object.
 * <p>
 * Typically used to bind the current {@code RSocket} (e.g., the sending or receiving socket)
 * to a reactive context or exchange object, enabling downstream components to access
 * connection-specific metadata, lifecycle hooks, or client identity.
 * <p>
 * Common use case: attaching the active socket to {@code PayloadExchange} for use
 * in interceptors, authorization mechanisms, or audit logging.
 */
public interface RSocketHolder {
    /**
     * Returns the {@link RSocket} instance associated with this object.
     *
     * @return the current {@link RSocket}, or {@code null} if not set
     */
    RSocket getRSocket();

    /**
     * Sets the {@link RSocket} instance to associate with this object.
     *
     * @param rSocket the {@link RSocket} to associate
     */
    void setRSocket(RSocket rSocket);
}
