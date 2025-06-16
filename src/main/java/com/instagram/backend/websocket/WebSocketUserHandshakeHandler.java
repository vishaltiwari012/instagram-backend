package com.instagram.backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Slf4j
public class WebSocketUserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        Principal user = (Principal) attributes.get("user");

        if (user != null) {
            log.info("Determined WebSocket Principal: {}", user.getName());
            return user;
        }

        log.warn("No user found in handshake attributes. Assigning 'anonymousUser'.");
        return () -> "anonymousUser";
    }
}
