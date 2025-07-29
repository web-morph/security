package com.github.webmorph.mixin;

import com.github.webmorph.security.account.model.Account;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

/**
 * Mixin for {@link SecurityExpressionRoot}
 * that overrides all {@code hasPermission(...)} methods to delegate permission checks
 * to the authenticated {@link Account}, which internally uses LuckPerms.
 *
 * <p>This allows using Spring Security expressions like:
 * <pre>
 *     {@code @PreAuthorize("hasPermission('some.permission')")}
 * </pre>
 * without requiring a custom {@link org.springframework.security.access.PermissionEvaluator}.
 * All permission checks are routed through {@link Account#hasPermission(String)}.
 *
 * <p>Note: This mixin assumes the authenticated principal is an instance of {@link Account}.
 * If not, a {@link ClassCastException} will be thrown.
 *
 * <p>This approach avoids using {@code PermissionEvaluator} and simplifies
 * expression-based access control for RSocket and HTTP security in a unified way.
 *
 * @see Account
 * @see SecurityExpressionRoot
 */
@Mixin(SecurityExpressionRoot.class)
public abstract class MixinSecurityExpressionRoot extends SecurityExpressionRoot {
    public MixinSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
    }

    public MixinSecurityExpressionRoot(Supplier<Authentication> authentication) {
        super(authentication);
    }

    @Shadow
    public abstract Object getPrincipal();

    @Overwrite
    public boolean hasPermission(Object target, Object permission) {
        return this.hasPermission((String) permission);
    }

    @Overwrite
    public boolean hasPermission(Object targetId, String targetType, Object permission) {
        return this.hasPermission((String) permission);
    }

    public boolean hasPermission(String permission) {
        return ((Account) this.getPrincipal()).hasPermission(permission);
    }

    public boolean hasGroup(String group) {
        return this.hasPermission("group." + group);
    }
}
