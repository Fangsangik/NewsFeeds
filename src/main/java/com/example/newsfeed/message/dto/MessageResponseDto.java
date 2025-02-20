package com.example.newsfeed.message.dto;

import com.example.newsfeed.message.entity.Message;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MessageResponseDto {
    private Long id;
    private String content;
    private Long memberId;

    @Builder
    public MessageResponseDto(Long id, String content, Long memberId) {
        this.id = id;
        this.content = content;
        this.memberId = memberId;
    }

    public static MessageResponseDto toDto(Message savedMessage) {
        return MessageResponseDto.builder()
                .id(savedMessage.getId())
                .content(savedMessage.getContent())
                .memberId(savedMessage.getMember().getId())
                .build();
    }
}
