package com.example.demo.DTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 房間（多人戰鬥）完整狀態。
 * 同時存 Redis（key = room:{roomId}）+ 廣播給訂閱該房間的 client。
 */
public class RoomDTO implements Serializable {

    public static final int MAX_MEMBERS = 4;

    private String roomId;
    private String name;            // 房名

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
    private Integer round;          // 從 1 開始
    private RoomPhase phase;        // PLAYER / BOSS
    private RoomOutcome outcome;    // ONGOING / VICTORY / DEFEAT / ABANDONED

    private List<String> log = new ArrayList<>();
    private Long createdAt;         // epoch ms
    private Long endedAt;           // 結算時間，null = 還沒結束

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getBossId() { return bossId; }
    public void setBossId(Long bossId) { this.bossId = bossId; }
    public String getBossCode() { return bossCode; }
    public void setBossCode(String bossCode) { this.bossCode = bossCode; }
    public String getBossName() { return bossName; }
    public void setBossName(String bossName) { this.bossName = bossName; }
    public String getBossIcon() { return bossIcon; }
    public void setBossIcon(String bossIcon) { this.bossIcon = bossIcon; }
    public Integer getBossMaxHp() { return bossMaxHp; }
    public void setBossMaxHp(Integer bossMaxHp) { this.bossMaxHp = bossMaxHp; }
    public Integer getBossHp() { return bossHp; }
    public void setBossHp(Integer bossHp) { this.bossHp = bossHp; }
    public Integer getBossAttack() { return bossAttack; }
    public void setBossAttack(Integer bossAttack) { this.bossAttack = bossAttack; }
    public Integer getBossDefense() { return bossDefense; }
    public void setBossDefense(Integer bossDefense) { this.bossDefense = bossDefense; }
    public Long getBossRewardGold() { return bossRewardGold; }
    public void setBossRewardGold(Long bossRewardGold) { this.bossRewardGold = bossRewardGold; }
    public List<RoomMemberDTO> getMembers() { return members; }
    public void setMembers(List<RoomMemberDTO> members) { this.members = members; }
    public Integer getRound() { return round; }
    public void setRound(Integer round) { this.round = round; }
    public RoomPhase getPhase() { return phase; }
    public void setPhase(RoomPhase phase) { this.phase = phase; }
    public RoomOutcome getOutcome() { return outcome; }
    public void setOutcome(RoomOutcome outcome) { this.outcome = outcome; }
    public List<String> getLog() { return log; }
    public void setLog(List<String> log) { this.log = log; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getEndedAt() { return endedAt; }
    public void setEndedAt(Long endedAt) { this.endedAt = endedAt; }
}
