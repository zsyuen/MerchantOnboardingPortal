package com.merchant.portal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Every incoming request passes through these rules before reaching any controller.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — not needed for stateless REST APIs that don't use cookies
                .csrf(csrf -> csrf.disable())

                // Apply custom CORS configuration defined in corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints — publicly accessible (login, MFA setup/verify)
                        .requestMatchers("/api/auth/login", "/api/auth/setup-totp", "/api/auth/verify-totp").permitAll()
                        // Merchant can submit an application without being logged in
                        .requestMatchers(HttpMethod.POST, "/api/applications").permitAll()
                        // Merchant can check their application status using a reference ID
                        .requestMatchers(HttpMethod.GET, "/api/applications/ref/**").permitAll()
                        // Document viewing — requires JWT (admin only)
                        .requestMatchers(HttpMethod.GET, "/api/documents/**").authenticated()
                        // All other endpoints require a valid JWT token
                        .anyRequest().authenticated()
                )

                // Stateless — no HTTP sessions created or used; every request must carry its own JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Define explicit 401 Unauthorized response for requests missing a valid JWT
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.getWriter().write("{\"error\": \"Unauthorized - Valid JWT token is missing or expired\"}");
                        })
                )

                // Use custom JWT filter to run before Spring's default login filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    //Use BCrypt as the password hashing algorithm
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //Controls which origins, methods and headers are allowed to make cross-origin requests
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Only allow requests from the Angular frontend
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        // Allow standard REST methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allow all headers (including Authorization for JWT)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // Allow credentials (e.g. Authorization header) to be sent cross-origin
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
