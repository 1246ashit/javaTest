package com.example.demo.dto;

import lombok.Getter;

/**
 * 大廳列表用，比 RoomDTO 精簡，不帶 log/members 細節。
 */
@Getter
public class RoomSummaryDTO {
    private String roomId;
    private String name;
    private String bossName;
    private String bossIcon;
    private Integer bossHp;
    private Integer bossMaxHp;
    private Integer memberCount;
    private Integer maxMembers;
    private RoomOutcome outcome;
    private Long createdAt;

    public static RoomSummaryDTO of(RoomDTO r) {
        RoomSummaryDTO s = new RoomSummaryDTO();
        s.roomId = r.getRoomId();
        s.name = r.getName();
        s.bossName = r.getBossName();
        s.bossIcon = r.getBossIcon();
        s.bossHp = r.getBossHp();
        s.bossMaxHp = r.getBossMaxHp();
        s.memberCount = (int) r.getMembers().stream().filter(m -> !m.isFled()).count();
        s.maxMembers = RoomDTO.MAX_MEMBERS;
        s.outcome = r.getOutcome();
        s.createdAt = r.getCreatedAt();
        return s;
    }
}
