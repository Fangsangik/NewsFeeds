package com.example.newsfeed.message.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Placeholder Redis pub/sub subscriber so {@link com.example.newsfeed.constants.config.RedisConfig}
 * can wire {@code listenerAdapter} without compilation errors. Real chat fan-out
 * logic can be added here later.
 */
@Slf4j
@Component
public class ChatMessageSubscriber {

    public void handleMessage(String message) {
        log.debug("[ChatMessageSubscriber] received: {}", message);
    }
}
