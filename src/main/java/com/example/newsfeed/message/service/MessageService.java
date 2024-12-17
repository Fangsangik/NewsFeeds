package com.example.newsfeed.message.service;

import com.example.newsfeed.config.MyWebSocketHandler;
import com.example.newsfeed.message.dto.MessageRequestDto;
import com.example.newsfeed.message.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MessageService {

    private final MyWebSocketHandler myWebSocketHandler;
    private final MessageRepository messageRepository;

    public MessageService(MyWebSocketHandler myWebSocketHandler, MessageRepository messageRepository) {
        this.myWebSocketHandler = myWebSocketHandler;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void sendMessage(MessageRequestDto requestDto) {
        log.info("메시지 저장 : Sender " + requestDto.getSenderId() + ", Receiver " + requestDto.getReceiverId() + ", content: " + requestDto.getMessage());

        String payLoad = "New Message from " + requestDto.getSenderId() + " : " + requestDto.getMessage();
        myWebSocketHandler.sendMessageToUser(requestDto.getReceiverId(), payLoad);
    }

    @Transactional(readOnly = true)
    public Page<MessageRequestDto> getMessageByUserId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageRequestDto> messages = messageRepository.findMemberById(memberId, pageable);

        return messages.map(message -> new MessageRequestDto(
            message.getSenderId(),
            message.getReceiverId(),
            message.getMessage(),
            message.getTimeStamp()
        ));
    }
}
