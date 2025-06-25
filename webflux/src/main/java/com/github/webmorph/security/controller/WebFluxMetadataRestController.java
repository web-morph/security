package com.github.webmorph.security.controller;

import com.github.webmorph.security.account.model.Account;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller that handles requests for retrieving metadata associated with the authenticated {@link Account}.
 * <p>
 * Supports both HTTP (`/account/get-metadata`) and RSocket (`account.get-metadata`) request mappings.
 * <p>
 * Requires the user to be authenticated. The metadata is looked up by key and returned as a string value.
 *
 * <p><strong>Endpoints:</strong></p>
 * <ul>
 *     <li>{@code POST /account/get-metadata} — HTTP endpoint</li>
 *     <li>{@code account.get-metadata} — RSocket route</li>
 * </ul>
 *
 * @see Account
 */
@RestController
public class WebFluxMetadataRestController {
    /**
     * Retrieves a metadata value for the authenticated account based on the provided key.
     *
     * @param account the authenticated account injected by Spring Security
     * @param request the metadata request containing the key (must be non-empty)
     * @return a Mono emitting the metadata value (wrapped in {@link GetMetadataResponse})
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/account/get-metadata")
    public Mono<GetMetadataResponse> getMetadata(@AuthenticationPrincipal Account account, @RequestBody GetMetadataRequest request) {
        return Mono.just(new GetMetadataResponse(account.getMetadata(request.key)));
    }

    /**
     * DTO representing the metadata key request.
     *
     * @param key the metadata key (must not be empty)
     */
    public record GetMetadataRequest(String key) {
    }

    /**
     * DTO representing the metadata response value.
     *
     * @param value the metadata value associated with the requested key
     */
    public record GetMetadataResponse(String value) {
    }
}
