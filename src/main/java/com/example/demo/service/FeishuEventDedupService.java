package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FeishuEventDedupService {

    private static final long EXPIRE_MILLIS = 10 * 60 * 1000L;
    private final ConcurrentHashMap<String, Long> processedEvents = new ConcurrentHashMap<>();

    public boolean isDuplicate(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        cleanup(now);
        Long existing = processedEvents.putIfAbsent(eventId, now);
        return existing != null;
    }

    private void cleanup(long now) {
        Iterator<Map.Entry<String, Long>> iterator = processedEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > EXPIRE_MILLIS) {
                iterator.remove();
            }
        }
    }
}
