package com.example.newsfeed.message.service;

import com.example.newsfeed.message.dto.MessagePayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * ChatMessagePublisher
 * redis를 통해 메시지를 다른 클라이언트에게 전달
 */

@Slf4j
@Service
public class ChatMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public ChatMessagePublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String channel, MessagePayload message) {
        try {
            redisTemplate.convertAndSend(channel, message);
            log.info("redis 채널 {}에 메시지 전송 : {}", channel, message);
        } catch (Exception e) {
            log.error("redis 채널 {}에 메시지 전송 중 오류 발생 : {}", channel, e.getMessage());
        }
    }
}
