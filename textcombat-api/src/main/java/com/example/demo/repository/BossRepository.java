package com.example.demo.repository;

import com.example.demo.entity.Boss;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BossRepository extends JpaRepository<Boss, Long> {
    List<Boss> findAllByOrderBySortOrderAscIdAsc();
    Optional<Boss> findByCode(String code);
}
