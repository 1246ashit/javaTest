package com.example.demo.service.battle;

import com.example.demo.dto.ActionContext;
import com.example.demo.dto.RoomAction;
import com.example.demo.dto.RoomDTO;
import com.example.demo.dto.RoomMemberDTO;

public interface ActionStrategy {
    RoomAction action();   // 我負責哪一種行動（拿來當 Map 的 key）
    void execute(RoomDTO r, RoomMemberDTO me, ActionContext ctx);
}