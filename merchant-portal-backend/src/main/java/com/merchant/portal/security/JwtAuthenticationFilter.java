package com.merchant.portal.security;

import com.merchant.portal.model.User;
import com.merchant.portal.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Read the Authorization header from the incoming request
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Check if the header exists and follows the "Bearer <token>" format
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // Strip the "Bearer " prefix to get the raw JWT string
            jwt = authorizationHeader.substring(7);
            try {
                // Extract the username embedded inside the JWT payload
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                // Token is malformed, expired, or tampered — silently ignore and continue
                // The request will fail the .anyRequest().authenticated() check further down
            }
        }

        // Proceed if we got a username AND the SecurityContext is not already authenticated
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Look up the user in the database to make sure they still exist
            Optional<User> userOptional = userRepository.findByUsername(username);

            // Validate the token signature and expiry, and confirm the user exists
            if (userOptional.isPresent() && jwtUtil.validateToken(jwt, username)) {
                User user = userOptional.get();

                // Build Spring Security's authentication token with the user's role
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );

                // Attach request metadata to the token
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Register the authenticated user in the SecurityContext & Spring Security treats this request as authenticated
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Always pass the request down to the next filter regardless of auth outcome
        filterChain.doFilter(request, response);
    }
}
