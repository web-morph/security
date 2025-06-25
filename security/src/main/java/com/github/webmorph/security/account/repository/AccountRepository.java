package com.github.webmorph.security.account.repository;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.webmorph.security.account.model.Account;
import lombok.RequiredArgsConstructor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.UserManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Reactive repository that bridges {@link LuckPerms} user management
 * with the {@link Account} domain model.
 * <p>
 * Provides non-blocking access to users by UUID or username,
 * with optional creation and caching of {@link Account} instances
 * using Caffeine for short-term reuse.
 */
@Service
@RequiredArgsConstructor
public class AccountRepository {
    private final LuckPerms luckPerms;
    private final PasswordEncoder passwordEncoder;

    /**
     * In-memory async cache of resolved {@link Account} instances,
     * expiring 5 minutes after last access.
     */
    private final AsyncCache<UUID, Account> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .buildAsync();

    /**
     * Checks whether a user exists by username.
     * <p>
     * Performs a case-insensitive lookup via LuckPerms,
     * and stores the resolved UUID in the reactive context for downstream use.
     *
     * @param username the username to check
     * @return {@code true} if user exists, {@code false} otherwise
     */
    public Mono<Boolean> existsByUsername(String username) {
        final String lowerCase = username.toLowerCase();
        return Mono.fromFuture(this.luckPerms.getUserManager().lookupUniqueId(lowerCase))
                .map(unused -> true)
                .switchIfEmpty(Mono.just(false));
    }

    /**
     * Checks whether a user exists by UUID.
     * <p>
     * Also stores the resolved username in the reactive context if found.
     *
     * @param uuid the UUID to check
     * @return {@code true} if user exists, {@code false} otherwise
     */
    public Mono<Boolean> existsByUuid(UUID uuid) {
        return Mono.fromFuture(this.luckPerms.getUserManager().lookupUsername(uuid))
                .map(unused -> true)
                .switchIfEmpty(Mono.just(false));
    }

    /**
     * Resolves and loads an {@link Account} by UUID.
     * <p>
     * Uses the username from the context if available. Falls back to {@link #findOrCreate(UUID, String)}.
     *
     * @param uuid the user's UUID
     * @return a {@link Mono} emitting the {@link Account} if found
     */
    public Mono<Account> findByUuid(UUID uuid) {
        return Mono.fromFuture(this.luckPerms.getUserManager().lookupUsername(uuid))
                .flatMap(username -> this.findOrCreate(uuid, username));
    }

    /**
     * Resolves and loads an {@link Account} by username.
     * <p>
     * Uses the UUID from the context if available. Falls back to {@link #findOrCreate(UUID, String)}.
     *
     * @param username the user's username
     * @return a {@link Mono} emitting the {@link Account} if found
     */
    public Mono<Account> findByUsername(String username) {
        final String lowerCase = username.toLowerCase();
        return Mono.fromFuture(this.luckPerms.getUserManager().lookupUniqueId(lowerCase))
                .flatMap(uuid -> this.findOrCreate(uuid, username));
    }

    /**
     * Loads or creates an {@link Account} from the LuckPerms user data.
     * <p>
     * If the account is cached, returns the cached version.
     * Otherwise, loads the LuckPerms user, wraps it in {@link Account},
     * and stores it in cache.
     *
     * @param uuid     the user UUID
     * @param username the username (used for loading from LuckPerms)
     * @return a {@link Mono} of {@link Account}
     */
    public Mono<Account> findOrCreate(UUID uuid, String username) {
        UserManager userManager = this.luckPerms.getUserManager();
        final String lowerCaseUsername = username.toLowerCase();
        return Mono.fromFuture(this.cache.get(uuid, (key, ignored) ->
                Mono.fromFuture(userManager.loadUser(uuid, lowerCaseUsername))
                        .map(user -> new Account(user, userManager, this.passwordEncoder))
                        .doOnNext(account -> account.setUsername(lowerCaseUsername))
                        .flatMap(account -> account.save().then(Mono.just(account)))
                        .toFuture()));
    }
}
