package com.instagram.backend.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registering WebSocket endpoint at /ws-notifications");

        registry.addEndpoint("/ws-notifications")
                .addInterceptors(authHandshakeInterceptor)
                .setHandshakeHandler(new WebSocketUserHandshakeHandler())
                .setAllowedOriginPatterns("*")
                .withSockJS();
        registry.addEndpoint("/ws-chat")    
                .addInterceptors(authHandshakeInterceptor)
                .setHandshakeHandler(new WebSocketUserHandshakeHandler())
                .setAllowedOriginPatterns("*")
                .withSockJS();


        log.info("WebSocket endpoint registered with SockJS and custom handshake handler.");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        log.info("Configuring message broker with /queue and /topic as simple brokers");

        registry.enableSimpleBroker("/queue", "/topic"); // topic used for group/public broadcast, queue for private
        registry.setApplicationDestinationPrefixes("/app"); // All @MessageMapping should begin with /app
        registry.setUserDestinationPrefix("/user"); // for private 1-1 communication

        log.info("Message broker configured successfully.");
    }
}
