package com.example.newsfeed.message.socket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePublisher {
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * WebSocket에서 받은 메시지를 Redis Pub/Sub을 이용해 발행하는 역할
     * @param channel
     * @param message
     */
    public void publish(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
        log.info("📤 Redis에 메시지 발행: {} → {}", channel, message);
    }
}
