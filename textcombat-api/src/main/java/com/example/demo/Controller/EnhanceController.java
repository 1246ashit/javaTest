package com.example.demo.Controller;

import com.example.demo.DTO.EnhanceRequest;
import com.example.demo.Security.CurrentUserHolder;
import com.example.demo.Services.EnhanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/enhance")
public class EnhanceController {

    private final EnhanceService enhanceService;

    public EnhanceController(EnhanceService enhanceService) {
        this.enhanceService = enhanceService;
    }

    @GetMapping("/preview/{inventoryItemId}")
    public ResponseEntity<?> preview(@PathVariable Long inventoryItemId) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            return ResponseEntity.ok(enhanceService.preview(userId, inventoryItemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> enhance(@RequestBody EnhanceRequest req) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            return ResponseEntity.ok(enhanceService.enhance(userId, req.getInventoryItemId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
