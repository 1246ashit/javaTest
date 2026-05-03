package com.example.demo.repository;

import com.example.demo.entity.GoldTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoldTransactionRepository extends JpaRepository<GoldTransaction, Long> {
    Page<GoldTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<GoldTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<GoldTransaction> findAllByUserIdOrderByCreatedAtDesc(Long userId , Pageable pageable);

}
