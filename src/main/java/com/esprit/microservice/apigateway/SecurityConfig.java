package com.esprit.microservice.apigateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity httpSecurity,
            Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter,
            CorsConfigurationSource corsConfigurationSource
    ) {
        return httpSecurity
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(
                                "/actuator/health",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/openapi/**"
                        ).permitAll()
                        // Public Endpoints
                        .pathMatchers(HttpMethod.POST, "/api/users").permitAll() // User registration
                        .pathMatchers(HttpMethod.GET, "/api/site-camping/getAll").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/site-camping/getsite/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/site-camping/*/availability").permitAll()
                        
                        // User Service
                        .pathMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .pathMatchers("/api/users/**").hasAnyRole("ADMIN", "CAMPER", "SITE_OWNER")
                        
                        // Camping Service
                        .pathMatchers(HttpMethod.POST, "/api/site-camping/addSite").hasAnyRole("ADMIN", "SITE_OWNER")
                        .pathMatchers(HttpMethod.PATCH, "/api/site-camping/updateSite/**").hasAnyRole("ADMIN", "SITE_OWNER")
                        .pathMatchers(HttpMethod.PATCH, "/api/site-camping/close/**").hasAnyRole("ADMIN", "SITE_OWNER")
                        .pathMatchers(HttpMethod.GET, "/api/site-camping/my-sites").hasAnyRole("ADMIN", "SITE_OWNER")
                        
                        // Booking Service
                        .pathMatchers(HttpMethod.POST, "/api/inscriptionsite/add").hasAnyRole("ADMIN", "CAMPER")
                        .pathMatchers(HttpMethod.PATCH, "/api/inscriptionsite/cancel/**").hasAnyRole("ADMIN", "CAMPER")
                        .pathMatchers(HttpMethod.GET, "/api/inscriptionsite/my-inscriptions/**").hasAnyRole("ADMIN", "CAMPER")
                        .pathMatchers(HttpMethod.PATCH, "/api/inscriptionsite/confirm-payment/**").hasAnyRole("ADMIN", "CAMPER", "SITE_OWNER")
                        
                        .pathMatchers(HttpMethod.GET, "/api/inscriptionsite/bySite/**").hasAnyRole("ADMIN", "SITE_OWNER")
                        .pathMatchers(HttpMethod.GET, "/api/inscriptionsite/my-camp-booking-list/**").hasAnyRole("ADMIN", "SITE_OWNER")
                        .pathMatchers(HttpMethod.GET, "/api/inscriptionsite/getAll").hasRole("ADMIN")
                        
                        // Event Service
                        .pathMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/events/*/registrations")
                        .hasAnyRole("USER", "ORGANIZER", "ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/events/*/registrations/*")
                        .hasAnyRole("USER", "ORGANIZER", "ADMIN")
                        .pathMatchers("/api/events/**").hasAnyRole("ADMIN", "ORGANIZER")
                        
                        // Notification Service
                        .pathMatchers(HttpMethod.GET, "/api/notifications/**").hasAnyRole("USER", "ORGANIZER", "ADMIN", "CAMPER", "SITE_OWNER")
                        .pathMatchers(HttpMethod.PATCH, "/api/notifications/**").hasAnyRole("USER", "ORGANIZER", "ADMIN", "CAMPER", "SITE_OWNER")
                        .pathMatchers("/api/notifications/**").hasAnyRole("ADMIN", "ORGANIZER", "SITE_OWNER")
                        
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") List<String> allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static final class KeycloakRealmRoleConverter
            implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Object realmAccessClaim = jwt.getClaims().get("realm_access");
            if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
                return authorities;
            }

            Object rolesClaim = realmAccess.get("roles");
            if (!(rolesClaim instanceof Collection<?> roles)) {
                return authorities;
            }

            roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::toUpperCase)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
            return authorities;
        }
    }
}
