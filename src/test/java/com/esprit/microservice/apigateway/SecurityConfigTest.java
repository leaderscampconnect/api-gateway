package com.esprit.microservice.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig.KeycloakRealmRoleConverter converter =
            new SecurityConfig.KeycloakRealmRoleConverter();

    @Test
    void convertsKeycloakRealmRolesToSpringAuthorities() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", "user-1", "realm_access", Map.of(
                        "roles", List.of("organizer", "user")
                ))
        );

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ORGANIZER", "ROLE_USER");
    }

    @Test
    void returnsNoAuthoritiesWhenRealmRolesAreMissing() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );

        assertThat(converter.convert(jwt)).isEmpty();
    }
}
