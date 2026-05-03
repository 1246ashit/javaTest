package com.example.demo.controller;

import com.example.demo.dto.ShopBuyRequest;
import com.example.demo.security.CurrentUserHolder;
import com.example.demo.service.ShopService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(shopService.listShopItems());
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buy(@RequestBody ShopBuyRequest req) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            int qty = req.getQuantity() == null ? 1 : req.getQuantity();
            return ResponseEntity.ok(shopService.buy(userId, req.getItemId(), qty));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
