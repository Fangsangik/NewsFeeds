package com.example.newsfeed.message.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MessageRequestDto {
    private String content;
    private Long memberId;

    public MessageRequestDto(String content, Long memberId) {
        this.content = content;
        this.memberId = memberId;
    }
}
