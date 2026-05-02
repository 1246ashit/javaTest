package com.example.demo.DTO;

import lombok.Getter;

@Getter
public class UserGoldResponse {
    private Long gold;

    public UserGoldResponse(Long gold) {
        this.gold = gold;
    }
}
