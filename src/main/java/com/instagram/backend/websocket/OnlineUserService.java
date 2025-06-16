package com.instagram.backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OnlineUserService {

    // Thread-safe set to track online users
    private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

    public void userConnected(Long userId) {
        onlineUsers.add(userId);
        log.info("User [{}] marked as online. Total online: {}", userId, onlineUsers.size());
    }

    public void userDisconnected(Long userId) {
        onlineUsers.remove(userId);
        log.info("User [{}] marked as offline. Total online: {}", userId, onlineUsers.size());
    }

    public boolean isUserOnline(Long userId) {
        boolean online = onlineUsers.contains(userId);
        log.debug("Checked online status for user [{}]: {}", userId, online);
        return online;
    }

    // for debugging or admin visibility
    public Set<Long> getAllOnlineUsers() {
        return Set.copyOf(onlineUsers);
    }
}
