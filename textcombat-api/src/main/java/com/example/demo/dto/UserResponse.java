package com.example.demo.dto;

import com.example.demo.entity.Role;
import com.example.demo.entity.UsersEntity;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class UserResponse {
    private Long id;
    private String username;
    private String displayName;
    private Long gold;
    private Set<String> roles;
    private Set<String> permissions;

    public static UserResponse from(UsersEntity user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.username = user.getUsername();
        r.displayName = user.getDisplayName();
        r.gold = user.getGold();
        r.roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        r.permissions = user.getPermissionCodes();
        return r;
    }
}
