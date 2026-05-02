package com.example.demo.DTO;

import com.example.demo.Entities.Boss;
import lombok.Getter;

@Getter
public class BossDTO {

    private Long id;

    private String code;

    private String name;

    private String description;

    private String icon;

    private Integer hp;

    private Integer attack;

    private Integer defense;

    private Long rewardGold;

    public static BossDTO of(Boss b) {
        BossDTO d = new BossDTO();
        d.id = b.getId();
        d.code = b.getCode();
        d.name = b.getName();
        d.description = b.getDescription();
        d.icon = b.getIcon();
        d.hp = b.getHp();
        d.attack = b.getAttack();
        d.defense = b.getDefense();
        d.rewardGold = b.getRewardGold();
        return d;
    }
}
