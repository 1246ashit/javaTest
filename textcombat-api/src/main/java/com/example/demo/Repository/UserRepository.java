package com.example.demo.Repository;
import com.example.demo.Entities.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface UserRepository extends JpaRepository<UsersEntity, Long>{
    // 登入用：依 username 查
    Optional<UsersEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
