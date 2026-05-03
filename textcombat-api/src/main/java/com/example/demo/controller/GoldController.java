package com.example.demo.controller;

import com.example.demo.dto.GoldGrantRequest;
import com.example.demo.dto.GoldTransactionDTO;
import com.example.demo.dto.UserGoldResponse;
import com.example.demo.entity.GoldTransaction;
import com.example.demo.entity.UsersEntity;
import com.example.demo.repository.GoldTransactionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.CurrentUserHolder;
import com.example.demo.security.RequirePermission;
import com.example.demo.service.GoldService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gold")
public class GoldController {

    private final GoldService goldService;
    private final GoldTransactionRepository txRepository;
    private final UserRepository userRepository;

    public GoldController(GoldService goldService,
            GoldTransactionRepository txRepository,
            UserRepository userRepository) {
        this.goldService = goldService;
        this.txRepository = txRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getMyGold() {
        Long userId = CurrentUserHolder.get().getId();
        return ResponseEntity.ok(new UserGoldResponse(goldService.getGold(userId)));
    }

    // 管理員手動發金幣（amount 可正可負）
    @PostMapping("/grant")
    @RequirePermission("GOLD_GRANT")
    public ResponseEntity<?> grant(@Valid @RequestBody GoldGrantRequest req) {
        long balance = goldService.changeGold(req.getUserId(), req.getAmount(),
                "GRANT", null, req.getNote());
        return ResponseEntity.ok(Map.of(
                "userId", req.getUserId(),
                "amount", req.getAmount(),
                "balance", balance));
    }

    // 管理員看全站交易紀錄；傳 userId 可過濾；最多 100 筆
    @GetMapping("/transactions")
    @RequirePermission("USER_READ_ALL")
    public ResponseEntity<Page<GoldTransactionDTO>> transactions(@RequestParam(required = false) Long userId,
            Pageable pageable) {

        Page<GoldTransaction> page = (userId == null)
                ? txRepository.findAllByOrderByCreatedAtDesc(pageable)
                : txRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);

        // 一次撈相關的 username（避免 N+1）
        Map<Long, String> userIdToName = userRepository.findAllById(
                page.stream().map(GoldTransaction::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(UsersEntity::getId, UsersEntity::getUsername));

        Page<GoldTransactionDTO> dtos = page.map(tx -> GoldTransactionDTO.of(tx, userIdToName.get(tx.getUserId())));

        return ResponseEntity.ok(dtos);
    }
}
