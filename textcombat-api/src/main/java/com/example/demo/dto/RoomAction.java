package com.example.demo.dto;

public enum RoomAction {
    ATTACK,
    USE_POTION,
    SKIP        // 跳過自己這回合（避免 AFK 卡住其他人）
}
