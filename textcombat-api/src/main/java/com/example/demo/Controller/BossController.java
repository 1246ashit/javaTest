package com.example.demo.Controller;

import com.example.demo.DTO.BossDTO;
import com.example.demo.Repository.BossRepository;
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
