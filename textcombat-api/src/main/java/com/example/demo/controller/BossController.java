package com.example.demo.controller;

import com.example.demo.dto.BossDTO;
import com.example.demo.repository.BossRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bosses")
public class BossController {

    private final BossRepository bossRepo;

    public BossController(BossRepository bossRepo) {
        this.bossRepo = bossRepo;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(
                bossRepo.findAllByOrderBySortOrderAscIdAsc().stream()
                        .map(BossDTO::of)
                        .toList()
        );
    }
}
