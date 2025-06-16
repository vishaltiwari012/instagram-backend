package com.instagram.backend.utils;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtpSessionStore {

    private final Map<String, String> store = new ConcurrentHashMap<>();

    public void save(String tempToken, String username) {
        store.put(tempToken, username);
    }

    public String getUsername(String tempToken) {
        return store.get(tempToken);
    }

    public void remove(String tempToken) {
        store.remove(tempToken);
    }

    public boolean exists(String tempToken) {
        return store.containsKey(tempToken);
    }
}
