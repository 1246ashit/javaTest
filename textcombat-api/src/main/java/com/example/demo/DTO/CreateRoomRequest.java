package com.example.demo.DTO;

public class CreateRoomRequest {
    private Long bossId;
    private String name;     // 房名，可空 → 用「{user}的隊伍」

    public Long getBossId() { return bossId; }
    public void setBossId(Long bossId) { this.bossId = bossId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
