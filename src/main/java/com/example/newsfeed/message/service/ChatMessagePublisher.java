package com.example.newsfeed.message.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * ChatMessagePublisher
 * redis를 통해 메시지를 다른 클라이언트에게 전달
 */
@Service
public class ChatMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public ChatMessagePublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }
}
