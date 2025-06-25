package com.github.webmorph.security.configuration;

import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Core RSocket security configuration.
 */
@AutoConfiguration
@ComponentScan("com.github.webmorph.security")
public class SecurityConfiguration {

    /**
     * Removes the default "ROLE_" prefix from granted authorities.
     * <p>
     * Allows role-based logic to use authority names directly without Spring's
     * {@code ROLE_} prefix requirement.
     *
     * @return the {@link GrantedAuthorityDefaults} bean with empty prefix
     */
    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults(Strings.EMPTY);
    }

    /**
     * Configures the default password encoder to use BCrypt.
     * <p>
     * This encoder is used to hash and validate passwords within the authentication flow.
     *
     * @return the configured {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
