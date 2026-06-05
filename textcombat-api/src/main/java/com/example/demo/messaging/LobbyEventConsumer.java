package com.example.demo.messaging;

import com.example.demo.service.RoomService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 大廳事件 consumer：從 Kafka 收訊息，廣播給訂閱 /topic/lobby 的前端。
 */
@Component
public class LobbyEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LobbyEventConsumer.class);

    private final RoomService roomService;
    private final SimpMessagingTemplate messaging;

    public LobbyEventConsumer(RoomService roomService, SimpMessagingTemplate messaging) {
        this.roomService = roomService;
        this.messaging = messaging;
    }

    @KafkaListener(topics = LobbyEventPublisher.TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void onLobbyEvent(LobbyEvent event) {
        log.debug("收到 lobby 事件: type={}, roomId={}", event.type(), event.roomId());
        // 暫時：不管事件型別，重推整個大廳給前端（前端還沒接事件局部更新）
        messaging.convertAndSend("/topic/lobby", roomService.listOpen());
    }
}