package com.example.demo.Repository;

import com.example.demo.Entities.Boss;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BossRepository extends JpaRepository<Boss, Long> {
    List<Boss> findAllByOrderBySortOrderAscIdAsc();
    Optional<Boss> findByCode(String code);
}
