package com.instagram.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    private final JwtService jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        log.info("Incoming request URI: {}", path);

        // Skip JWT auth for WebSocket handshake endpoint
        if (path.startsWith("/ws-notifications")) {
            log.debug("Skipping JWT filter for WebSocket handshake endpoint: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            log.warn("Missing or invalid Authorization header: {}", authHeader);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(TOKEN_PREFIX.length());
        String username = null;
        try {
            username = jwtTokenProvider.extractUsername(token);
            log.debug("Extracted username from token: {}", username);
        } catch (Exception e) {
            log.error("Error extracting username from token", e);
        }

        if (username == null) {
            log.warn("Username extracted from token is null, skipping authentication");
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            // If you want to load userDetails from DB, uncomment below:
            // UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            List<String> rolesFromToken = null;
            try {
                rolesFromToken = jwtTokenProvider.extractRoles(token);
                log.debug("Roles extracted from token: {}", rolesFromToken);
            } catch (Exception e) {
                log.error("Error extracting roles from token", e);
            }

            if (rolesFromToken == null) {
                log.warn("No roles extracted from token, skipping authentication");
                filterChain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> authorities = rolesFromToken.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    username,
                    "", // Password not needed here
                    authorities
            );

            if (jwtTokenProvider.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("Authenticated user '{}', security context updated", username);
            } else {
                log.warn("JWT token validation failed for user '{}'", username);
            }
        } else {
            log.debug("Security context already contains authentication for user '{}'", username);
        }

        filterChain.doFilter(request, response);
    }
}
