package com.example.demo.DTO;

import com.example.demo.Entities.Role;
import com.example.demo.Entities.UsersEntity;

import java.util.Set;
import java.util.stream.Collectors;

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

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public Long getGold() { return gold; }
    public Set<String> getRoles() { return roles; }
    public Set<String> getPermissions() { return permissions; }
}
