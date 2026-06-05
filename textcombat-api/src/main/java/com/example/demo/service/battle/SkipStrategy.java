package com.example.demo.service.battle;

import org.springframework.stereotype.Component;

import com.example.demo.dto.ActionContext;
import com.example.demo.dto.RoomAction;
import com.example.demo.dto.RoomDTO;
import com.example.demo.dto.RoomMemberDTO;

@Component
public class SkipStrategy implements ActionStrategy {
    public RoomAction action() { return RoomAction.SKIP; }

    public void execute(RoomDTO r, RoomMemberDTO me, ActionContext ctx) {
        r.getLog().add(String.format("第 %d 回合：%s 選擇跳過",
                r.getRound(), BattleText.displayOf(me)));
    }
}