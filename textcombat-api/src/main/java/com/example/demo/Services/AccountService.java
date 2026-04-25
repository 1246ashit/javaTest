package com.example.demo.Services;

import com.example.demo.DTO.LoginRequest;
import com.example.demo.DTO.RegisterRequest;
import com.example.demo.Entities.UsersEntity;

public interface AccountService {

    UsersEntity register(RegisterRequest req);

    UsersEntity login(LoginRequest req);
}
