package com.example.demo.Services;

import com.example.demo.DTO.RoomAction;
import com.example.demo.DTO.RoomDTO;
import com.example.demo.DTO.RoomSummaryDTO;

import java.util.List;

public interface RoomService {

    /** 開房，建立者自動加入。 */
    RoomDTO createRoom(Long userId, Long bossId, String name);

    /** 加入既有房間。 */
    RoomDTO joinRoom(Long userId, String roomId);

    /** 離開（含逃跑）。離開後若房間沒人就清掉。 */
    RoomDTO leaveRoom(Long userId, String roomId);

    /** 玩家行動（攻擊／喝藥／跳過）。 */
    RoomDTO act(Long userId, String roomId, RoomAction action, Long inventoryItemId);

    /** 大廳：列所有 ONGOING 房間。 */
    List<RoomSummaryDTO> listOpen();

    /** 取單一房間（供前端剛進房時讀一次）。 */
    RoomDTO getRoom(String roomId);
}
