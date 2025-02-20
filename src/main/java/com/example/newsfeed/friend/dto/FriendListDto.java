package com.example.newsfeed.friend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FriendListDto {
    private Long id;
    private String name;


    public FriendListDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
