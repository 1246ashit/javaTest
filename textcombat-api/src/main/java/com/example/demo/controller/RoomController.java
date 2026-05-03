package com.example.demo.controller;

import com.example.demo.dto.CreateRoomRequest;
import com.example.demo.dto.RoomActionRequest;
import com.example.demo.security.CurrentUserHolder;
import com.example.demo.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<?> listOpen() {
        return ResponseEntity.ok(roomService.listOpen());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRoomRequest req) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            return ResponseEntity.ok(roomService.createRoom(userId, req.getBossId(), req.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {
        try {
            return ResponseEntity.ok(roomService.getRoom(roomId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> join(@PathVariable String roomId) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            return ResponseEntity.ok(roomService.joinRoom(userId, roomId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leave(@PathVariable String roomId) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            return ResponseEntity.ok(roomService.leaveRoom(userId, roomId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/action")
    public ResponseEntity<?> action(@PathVariable String roomId, @RequestBody RoomActionRequest req) {
        try {
            Long userId = CurrentUserHolder.get().getId();
            return ResponseEntity.ok(
                    roomService.act(userId, roomId, req.getAction(), req.getInventoryItemId())
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
