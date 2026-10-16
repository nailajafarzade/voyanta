package com.voyanta.common.config;

import com.voyanta.common.security.ApiAccessDeniedHandler;
import com.voyanta.common.security.ApiAuthenticationEntryPoint;
import com.voyanta.common.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
// Enables @PreAuthorize on controllers. URL rules below are the first line of defence;
// method security is the second, so a personalized endpoint stays protected even if
// someone later adds its path to PUBLIC_ENDPOINTS.
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/api/survey/**",
            "/api/plans/generate",
            "/api/plans/*/status",
            "/api/plans/*/claim",
            "/api/plans/*",
            "/api/destinations/**",
            "/api/homepage/**",
            "/actuator/health",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    /**
     * Unauthenticated requests to a protected endpoint are rejected by the filter chain
     * BEFORE a controller runs. Spring's default entry point answers with an empty body,
     * which is not our API shape - the client could only tell success from failure by the
     * status code. Both handlers now write the standard ApiResponse envelope, so even a
     * 403 is parseable like every other error. The status codes themselves are unchanged.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}