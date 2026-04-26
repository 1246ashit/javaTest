package com.example.demo.DTO;

import java.io.Serializable;

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
    private Long joinedAt;           // 進房時的回合數（< joinedAtRound 時就是本回合視為已動）

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Integer getHp() { return hp; }
    public void setHp(Integer hp) { this.hp = hp; }
    public Integer getMaxHp() { return maxHp; }
    public void setMaxHp(Integer maxHp) { this.maxHp = maxHp; }
    public Integer getAttack() { return attack; }
    public void setAttack(Integer attack) { this.attack = attack; }
    public Integer getDefense() { return defense; }
    public void setDefense(Integer defense) { this.defense = defense; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public boolean isFled() { return fled; }
    public void setFled(boolean fled) { this.fled = fled; }
    public boolean isActedThisRound() { return actedThisRound; }
    public void setActedThisRound(boolean actedThisRound) { this.actedThisRound = actedThisRound; }
    public Long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Long joinedAt) { this.joinedAt = joinedAt; }
}
