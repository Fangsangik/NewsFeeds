package com.example.newsfeed.constants.config;

import com.example.newsfeed.message.service.ChatMessageSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisConfig
 * redis의 pub/sub 메시지 리스너 설정
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * redisMessageListenerContainer
     * Redis의 pub/sub 메시징 기능을 활용해, 특정 주체에 대한 메시지를 비동기 수신
     *
     *
     * @param redisConnectionFactory : Redis의 연결을 서정
     * @param messageListenerAdapter : 메세지를 처리하는 Listener를 어뎁터 형태로 등록
     *
     * ChannelTopic : 특정 채널이나 패턴을 구독
     * @return
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter, new ChannelTopic("chat"));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(ChatMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //StringRedisSerializer : String 타입의 key를 직렬화
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //GenericJackson2JsonRedisSerializer : Object 타입의 value를 직렬화
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }
}
