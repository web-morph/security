package com.github.webmorph.security.account.model;

import com.github.webmorph.security.configuration.bearer.BearerReactiveAuthenticationManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.query.QueryOptions;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Domain model representing an authenticated user account.
 * <p>
 * Wraps a LuckPerms {@link User} and provides convenient methods to manage
 * groups, permissions, metadata, and identity in a reactive-friendly way.
 * <p>
 * Also includes logic for password validation and persistence through
 * {@link UserManager}.
 */
@RequiredArgsConstructor
public class Account {
    @Getter
    private final User user;
    private final UserManager userManager;
    private final PasswordEncoder passwordEncoder;
    private final BearerReactiveAuthenticationManager authenticationManager;
    private final AtomicBoolean usernameUpdated = new AtomicBoolean(false);

    /**
     * Returns all inherited groups for this user (non-contextual).
     *
     * @return a collection of {@link Group} objects
     */
    public Collection<Group> getGroups() {
        return this.user.getInheritedGroups(QueryOptions.nonContextual());
    }

    /**
     * Checks whether the user is a member of the specified group.
     *
     * @param group the {@link InheritanceNode} representing the group
     * @return true if the user has that group assigned
     */
    public boolean hasGroup(InheritanceNode group) {
        return this.hasPermission("group." + group.getGroupName());
    }

    /**
     * Adds the user to the specified group.
     *
     * @param group the group node to assign
     * @return true if operation was successful
     */
    public boolean addGroup(InheritanceNode group) {
        return this.user.data().add(group).wasSuccessful();
    }

    /**
     * Removes the user from the specified group.
     *
     * @param group the group node to remove
     * @return true if removal succeeded
     */
    public boolean removeGroup(InheritanceNode group) {
        return this.user.data().remove(group).wasSuccessful();
    }

    /**
     * Checks if the user has a specific permission.
     *
     * @param permission the permission string (e.g., "admin.access")
     * @return true if permission is granted
     */
    public boolean hasPermission(String permission) {
        return this.user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    /**
     * Adds a permission node to the user.
     *
     * @param permission the {@link PermissionNode} to add
     * @return true if successful
     */
    public boolean addPermission(PermissionNode permission) {
        return this.user.data().add(permission).wasSuccessful();
    }

    /**
     * Removes a permission node from the user.
     *
     * @param permission the {@link PermissionNode} to remove
     * @return true if successful
     */
    public boolean removePermission(PermissionNode permission) {
        return this.user.data().remove(permission).wasSuccessful();
    }

    /**
     * Retrieves the value of a metadata entry by key.
     *
     * @param key the metadata key
     * @return the associated value, or {@code null} if not found
     */
    public String getMetadata(String key) {
        return this.user.getCachedData().getMetaData().getMetaValue(key);
    }

    /**
     * Returns metadata value or default if not found.
     *
     * @param key          metadata key
     * @param defaultValue fallback value if metadata is missing
     * @return metadata or default
     */
    public String getMetadata(String key, String defaultValue) {
        String metadata = this.getMetadata(key);
        return metadata == null ? defaultValue : metadata;
    }

    /**
     * Sets a metadata value by key. Replaces previous entry if it exists.
     *
     * @param key   metadata key
     * @param value metadata value
     * @return true if operation was successful
     */
    public boolean setMetadata(String key, String value) {
        this.removeMetadata(key);
        return this.user.data().add(MetaNode.builder(key, value).build()).wasSuccessful();
    }

    /**
     * Removes all metadata nodes associated with the given key.
     *
     * @param key metadata key to remove
     */
    public void removeMetadata(String key) {
        NodeMap data = this.user.data();
        for (Node node : data.toCollection()) {
            if (node instanceof MetaNode meta && meta.getMetaKey().equals(key)) {
                data.remove(meta);
            }
        }
    }

    /**
     * Validates a raw password against the stored bcrypt hash in metadata.
     *
     * @param password the plain-text password
     * @return true if matches
     */
    public boolean passwordMatches(String password) {
        return this.passwordEncoder.matches(password, this.getMetadata("password"));
    }

    /**
     * Sets the password hash (not raw password).
     *
     * @param password raw password to encode and store
     * @return true if metadata updated
     */
    public boolean setPassword(String password) {
        return this.setMetadata("password", this.passwordEncoder.encode(password));
    }

    /**
     * Sets the username in metadata
     *
     * @param username the new username
     * @return true if metadata updated
     */
    public boolean setUsername(String username) {
        this.usernameUpdated.set(true);
        return this.setMetadata("username", username.toLowerCase());
    }

    /**
     * Returns the globally unique identifier (UUID) associated with this account.
     * <p>
     * This value is provided by LuckPerms and is immutable for each user.
     *
     * @return the user's {@link UUID}
     */
    public UUID getUuid() {
        return this.user.getUniqueId();
    }

    /**
     * Returns the username associated with this account.
     * <p>
     * The username is stored as metadata (under the {@code "username"} key)
     * and may differ from the username resolved by LuckPerms internally.
     * <p>
     * Used as a logical identifier for display, authentication, or personalization.
     *
     * @return the stored username, or {@code null} if not present
     */
    public String getUsername() {
        return this.getMetadata("username");
    }

    /**
     * Saves the current account to the LuckPerms backend.
     * <p>
     * If the username has changed, triggers {@link UserManager#savePlayerData(UUID, String)} first.
     *
     * @return a {@link Mono} that completes when persistence is done
     */
    public Mono<Void> save() {
        return Mono.fromFuture(() -> this.usernameUpdated.getAndSet(false) ?
                        this.userManager.savePlayerData(this.getUuid(), this.getUsername()) :
                        CompletableFuture.completedFuture(null))
                .then(Mono.fromFuture(() -> this.userManager.saveUser(user)));
    }

    /**
     * Generates a JWT token for the current authenticated account.
     * <p>
     * The token can optionally have an extended expiration time if {@code rememberMe} is set to {@code true}.
     * This token can later be used for stateless authentication of the user.
     *
     * @param rememberMe whether to generate a long-lived "remember me" token
     * @return a {@link Mono} emitting the generated JWT as a {@link String}
     */
    public Mono<String> generateToken(boolean rememberMe) {
        return this.authenticationManager.generateJWT(this, rememberMe);
    }
}
