package com.example.newsfeed.message.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ChatMessageSubscriber
 * redis에서 수신한 메시지를 처리
 */
@Slf4j
@Service
public class ChatMessageSubscriber {
    public void handleMessage(String message) {
        log.info("메시지 수신: {}", message);
    }
}
