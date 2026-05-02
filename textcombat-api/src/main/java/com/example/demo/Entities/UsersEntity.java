package com.example.demo.Entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table(name = "users")
@Getter                      
@Setter                      
@NoArgsConstructor
public class UsersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", length = 50)
    private String displayName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Long gold = 0L;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Version
    private Long version;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // 攤平所有角色底下的權限 code
    public Set<String> getPermissionCodes() {
        return roles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }

    public boolean hasPermission(String code) {
        return getPermissionCodes().contains(code);
    }
}
