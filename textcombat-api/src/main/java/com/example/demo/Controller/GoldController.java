package com.example.demo.Controller;

import com.example.demo.DTO.GoldGrantRequest;
import com.example.demo.DTO.GoldTransactionDTO;
import com.example.demo.DTO.UserGoldResponse;
import com.example.demo.Entities.GoldTransaction;
import com.example.demo.Entities.UsersEntity;
import com.example.demo.Repository.GoldTransactionRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Security.CurrentUserHolder;
import com.example.demo.Security.RequirePermission;
import com.example.demo.Services.GoldService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> grant(@RequestBody GoldGrantRequest req) {
        try {
            if (req.getUserId() == null || req.getAmount() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId 與 amount 為必填"));
            }
            long balance = goldService.changeGold(req.getUserId(), req.getAmount(),
                    "GRANT", null, req.getNote());
            return ResponseEntity.ok(Map.of(
                    "userId", req.getUserId(),
                    "amount", req.getAmount(),
                    "balance", balance
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    // 管理員看全站交易紀錄；傳 userId 可過濾；最多 100 筆
    @GetMapping("/transactions")
    @RequirePermission("USER_READ_ALL")
    public ResponseEntity<?> transactions(@RequestParam(required = false) Long userId) {
        List<GoldTransaction> list = (userId == null)
                ? txRepository.findTop100ByOrderByCreatedAtDesc()
                : txRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId);

        // 一次撈相關的 username（避免 N+1）
        Map<Long, String> userIdToName = userRepository.findAllById(
                list.stream().map(GoldTransaction::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(UsersEntity::getId, UsersEntity::getUsername));

        List<GoldTransactionDTO> dtos = list.stream()
                .map(tx -> GoldTransactionDTO.of(tx, userIdToName.get(tx.getUserId())))
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
