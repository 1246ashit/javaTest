package com.example.demo.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 房間（多人戰鬥）完整狀態。
 * 同時存 Redis（key = room:{roomId}）+ 廣播給訂閱該房間的 client。
 */
@Data
public class RoomDTO implements Serializable {

    public static final int MAX_MEMBERS = 4;

    private String roomId;
    private String name; // 房名

    // BOSS 快照（凍結在開房當下）
    private Long bossId;
    private String bossCode;
    private String bossName;
    private String bossIcon;
    private Integer bossMaxHp;
    private Integer bossHp;
    private Integer bossAttack;
    private Integer bossDefense;
    private Long bossRewardGold;

    // 成員與進度
    private List<RoomMemberDTO> members = new ArrayList<>();
    private Integer round; // 從 1 開始
    private RoomPhase phase; // PLAYER / BOSS
    private RoomOutcome outcome; // ONGOING / VICTORY / DEFEAT / ABANDONED

    private List<String> log = new ArrayList<>();
    private Long createdAt; // epoch ms
    private Long endedAt; // 結算時間，null = 還沒結束

}
