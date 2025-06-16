package com.instagram.backend.websocket;

import com.instagram.backend.entity.User;
import com.instagram.backend.exception.UserNotFoundException;
import com.instagram.backend.repository.UserRepository;
import com.instagram.backend.security.CustomUserDetailsService;
import com.instagram.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService tokenProvider;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("Request is not an instance of ServletServerHttpRequest");
            return false;
        }

        String query = servletRequest.getServletRequest().getQueryString();
        log.info("Incoming WebSocket handshake query: {}", query);

        if (query == null || !query.contains("token=")) {
            log.warn("WebSocket handshake query does not contain token.");
            return false;
        }

        String token = query.substring(query.indexOf("token=") + 6);
        log.info("Extracted token from query: {}", token);

        String username = tokenProvider.extractUsername(token);
        if (username == null) {
            log.warn("Token does not contain a valid username.");
            return false;
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null) {
            log.warn("UserDetails not found for username: {}", username);
            return false;
        }

        if (!tokenProvider.validateToken(token, userDetails)) {
            log.warn("Invalid or expired token for user: {}", username);
            return false;
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found in database: {}", username);
                    return new UserNotFoundException("User not found in database: " + username);
                });

        attributes.put("token", token);
        attributes.put("user", new StompPrincipal(user.getId().toString()));

        log.info("WebSocket handshake authorized for userId: {}", user.getId());
        return true;
    }


    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // No operation after handshake
    }

    public static class StompPrincipal implements Principal {
        private final String name;

        public StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
