package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.UsersEntity;

public interface AccountService {

    UsersEntity register(RegisterRequest req);

    UsersEntity login(LoginRequest req);
}
