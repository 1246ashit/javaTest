package com.example.demo.Controller;

import com.example.demo.DTO.DiscardRequest;
import com.example.demo.DTO.EquipRequest;
import com.example.demo.DTO.UnequipRequest;
import com.example.demo.DTO.UseRequest;
import com.example.demo.Security.CurrentUserHolder;
import com.example.demo.Services.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        Long userId = CurrentUserHolder.get().getId();
        return ResponseEntity.ok(inventoryService.listInventory(userId));
    }

    @PostMapping("/equip")
    public ResponseEntity<?> equip(@RequestBody EquipRequest req) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            inventoryService.equip(userId, req.getInventoryItemId(), req.getSlotIndex());
            return ResponseEntity.ok(inventoryService.listInventory(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/unequip")
    public ResponseEntity<?> unequip(@RequestBody UnequipRequest req) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            inventoryService.unequip(userId, req.getSlotIndex());
            return ResponseEntity.ok(inventoryService.listInventory(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/use")
    public ResponseEntity<?> use(@RequestBody UseRequest req) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            String msg = inventoryService.useSlot(userId, req.getSlotIndex());
            return ResponseEntity.ok(Map.of(
                    "message", msg,
                    "inventory", inventoryService.listInventory(userId)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/discard")
    public ResponseEntity<?> discard(@RequestBody DiscardRequest req) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            inventoryService.discard(userId, req.getInventoryItemId(), req.getQuantity());
            return ResponseEntity.ok(inventoryService.listInventory(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
