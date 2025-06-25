package com.github.webmorph.security.configuration.bearer;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.webmorph.security.account.model.Account;
import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A custom {@link Authentication} implementation representing a JWT-authenticated session.
 * <p>
 * Wraps the {@link DecodedJWT} credentials and a domain-specific {@link Account} principal.
 * Also exposes {@link MetaNode}-based metadata from LuckPerms via {@link #getDetails()}.
 * <p>
 * Used in Spring Security contexts where bearer token authorization is applied
 * at the RSocket or HTTP layer.
 */
@Getter
@Setter
public class BearerAuthenticationToken implements Authentication {
    private final Account principal;
    private final DecodedJWT credentials;
    private boolean authenticated;

    public BearerAuthenticationToken(Account principal, DecodedJWT credentials) {
        this.principal = principal;
        this.credentials = credentials;
    }

    /**
     * Returns the authorities granted to the user based on group membership.
     * <p>
     * Each group name is converted to a {@link SimpleGrantedAuthority}.
     *
     * @return an unmodifiable collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.principal.getGroups()
                .stream()
                .map(group -> new SimpleGrantedAuthority(group.getName()))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns additional user metadata exposed through LuckPerms {@link MetaNode}s.
     * <p>
     * This information can be used for fine-grained access control, client personalization,
     * or auditing purposes.
     *
     * @return a collection of {@link MetaNode} instances attached to the authenticated user
     */
    @Override
    public Collection<MetaNode> getDetails() {
        return this.principal.getUser().getNodes(NodeType.META);
    }

    /**
     * Returns the username of the authenticated user.
     *
     * @return the principal's username
     */
    @Override
    public String getName() {
        return this.principal.getUsername();
    }
}