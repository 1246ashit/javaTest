package com.example.demo.messaging;

import com.example.demo.dto.RoomSummaryDTO;

/**
 * 大廳事件 — 透過 Kafka 傳遞、最終推給前端訂閱 /topic/lobby 的玩家。
 *
 * <p>三種型別：
 * <ul>
 *   <li>ROOM_CREATED：新房開了，room 帶完整摘要</li>
 *   <li>ROOM_UPDATED：房內狀態變動（加入/離開/戰鬥），room 帶最新摘要</li>
 *   <li>ROOM_CLOSED：房結束（勝負/被拋棄），room 可為 null</li>
 * </ul>
 */
public record LobbyEvent(
        EventType type,
        String roomId,
        RoomSummaryDTO room) {

    public enum EventType {
        ROOM_CREATED,
        ROOM_UPDATED,
        ROOM_CLOSED
    }

    public static LobbyEvent created(RoomSummaryDTO room) {
        return new LobbyEvent(EventType.ROOM_CREATED, room.getRoomId(), room);
    }

    public static LobbyEvent updated(RoomSummaryDTO room) {
        return new LobbyEvent(EventType.ROOM_UPDATED, room.getRoomId(), room);
    }

    public static LobbyEvent closed(String roomId) {
        return new LobbyEvent(EventType.ROOM_CLOSED, roomId, null);
    }
}
