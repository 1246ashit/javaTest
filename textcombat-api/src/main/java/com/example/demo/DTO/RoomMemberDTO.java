package com.example.demo.DTO;

import java.io.Serializable;
import lombok.Data;

@Data
public class RoomMemberDTO implements Serializable {

    private Long userId;
    private String username;
    private String displayName;

    private Integer hp;
    private Integer maxHp;
    private Integer attack;
    private Integer defense;

    private boolean alive;
    private boolean fled;            // 逃跑（已離開房間）
    private boolean actedThisRound;  // 本回合是否已行動
    private Long joinedAt;   // 進房時的時間戳（epoch ms）

}
