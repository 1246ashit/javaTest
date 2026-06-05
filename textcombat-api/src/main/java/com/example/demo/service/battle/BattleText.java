package com.example.demo.service.battle;

import com.example.demo.dto.RoomMemberDTO;

public final class BattleText {
    private BattleText() {}
    public static String displayOf(RoomMemberDTO me) {
        return me.getDisplayName() != null && !me.getDisplayName().isBlank() ? me.getDisplayName() : me.getUsername();
    }
}