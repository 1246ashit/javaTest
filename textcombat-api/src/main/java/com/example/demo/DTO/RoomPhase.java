package com.example.demo.DTO;

public enum RoomPhase {
    PLAYER,    // 玩家階段：所有活著的玩家依序行動
    BOSS       // BOSS 階段：BOSS 動一次後回到 PLAYER
}
