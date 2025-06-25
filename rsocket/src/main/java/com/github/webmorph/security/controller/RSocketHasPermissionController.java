package com.github.webmorph.security.controller;

import com.github.webmorph.security.account.model.Account;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

/**
 * Controller that provides permission-checking functionality for the authenticated {@link Account}.
 * <p>
 * Supports both HTTP and RSocket endpoints for checking whether the current user has a specific permission.
 *
 * <p><strong>Endpoints:</strong></p>
 * <ul>
 *     <li>{@code POST /account/has-permission} — HTTP endpoint</li>
 *     <li>{@code account.has-permission} — RSocket route</li>
 * </ul>
 *
 * <p>Access to these endpoints is restricted to authenticated users via {@code @PreAuthorize("isAuthenticated()")}.
 *
 * @see Account#hasPermission(String)
 */
@Controller
public class RSocketHasPermissionController {
    /**
     * Checks if the authenticated account has the specified permission.
     *
     * @param account the authenticated account, injected via {@link AuthenticationPrincipal}
     * @param request the permission check request (must contain a non-empty permission string)
     * @return a {@link Mono} emitting {@link HasPermissionResponse} with the result
     */
    @PreAuthorize("isAuthenticated()")
    @MessageMapping("account.has-permission")
    public Mono<HasPermissionResponse> hasPermission(@AuthenticationPrincipal Account account, @Validated @Payload HasPermissionRequest request) {
        return Mono.just(new HasPermissionResponse(account.hasPermission(request.permission)));
    }

    /**
     * DTO representing a permission check request.
     *
     * @param permission the permission string to check (must not be empty)
     */
    public record HasPermissionRequest(
            String permission
    ) {
    }

    /**
     * DTO representing the permission check result.
     *
     * @param result {@code true} if the account has the permission, {@code false} otherwise
     */
    public record HasPermissionResponse(boolean result) {
    }
}
