package com.legalaid.backend.websocket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class PresenceService {

    // email -> number of active WS sessions
    private final Map<String, AtomicInteger> sessions =
            new ConcurrentHashMap<>();

    public void userConnected(String email) {
        sessions
            .computeIfAbsent(email, e -> new AtomicInteger(0))
            .incrementAndGet();
    }

    public void userDisconnected(String email) {
        AtomicInteger count = sessions.get(email);
        if (count == null) return;

        int remaining = count.decrementAndGet();

        if (remaining <= 0) {
            sessions.remove(email);
        }
    }

    public boolean isOnline(String email) {
        return sessions.containsKey(email);
    }

    public Set<String> getOnlineUsers() {
        return sessions.keySet();
    }
}
