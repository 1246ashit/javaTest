package com.example.demo.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private final String token;
    private final UserResponse user;
}
