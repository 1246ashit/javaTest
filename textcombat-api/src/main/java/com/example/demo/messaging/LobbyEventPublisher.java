package com.example.demo.messaging;

import com.example.demo.dto.RoomSummaryDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 大廳事件 publisher：把事件丟進 Kafka topic，consumer 負責真正廣播給前端。
 * <p>
 * 用 roomId 當 Kafka partition key，同一房的事件保證有序消費。
 */
@Component
public class LobbyEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LobbyEventPublisher.class);

    public static final String TOPIC = "lobby-events";

    private final KafkaTemplate<String, LobbyEvent> kafkaTemplate;

    public LobbyEventPublisher(KafkaTemplate<String, LobbyEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** 房間剛開（房間摘要當作初始狀態送出去）。 */
    public void publishCreated(RoomSummaryDTO room) {
        send(LobbyEvent.created(room));
    }

    /** 房內狀態變動（加入/離開/戰鬥），送最新摘要。 */
    public void publishUpdated(RoomSummaryDTO room) {
        send(LobbyEvent.updated(room));
    }

    /** 房間結束（勝負/被拋棄）。 */
    public void publishClosed(String roomId) {
        send(LobbyEvent.closed(roomId));
    }

    private void send(LobbyEvent event) {
        kafkaTemplate.send(TOPIC, event.roomId(), event);
        log.debug("發送 lobby 事件: type={}, roomId={}", event.type(), event.roomId());
    }
}
