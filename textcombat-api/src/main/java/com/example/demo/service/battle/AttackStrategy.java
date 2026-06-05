package com.example.demo.service.battle;

import org.springframework.stereotype.Component;

import com.example.demo.dto.ActionContext;
import com.example.demo.dto.RoomAction;
import com.example.demo.dto.RoomDTO;
import com.example.demo.dto.RoomMemberDTO;

@Component
public class AttackStrategy implements ActionStrategy {
    public RoomAction action() { return RoomAction.ATTACK; }

    public void execute(RoomDTO r, RoomMemberDTO me, ActionContext ctx) {
        int dmg = Math.max(1, me.getAttack() - r.getBossDefense());
        int newHp = Math.max(0, r.getBossHp() - dmg);
        r.getLog().add(String.format("第 %d 回合：%s 攻擊造成 %d 傷害（%s HP %d → %d）",
                r.getRound(), BattleText.displayOf(me), dmg, r.getBossName(), r.getBossHp(), newHp));
        r.setBossHp(newHp);
    }
}
