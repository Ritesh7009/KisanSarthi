package com.kisansarthi.slot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

@Component
public class SlotEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SlotEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public SlotEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishAfterCommit(String mandiId, String eventType, Object payload) {
        Runnable broadcastAction = () -> {
            try {
                Map<String, Object> message = new HashMap<>();
                message.put("event", eventType);
                message.put("mandiId", mandiId);
                message.put("data", payload);
                message.put("timestamp", System.currentTimeMillis());

                if (mandiId != null && !mandiId.isBlank()) {
                    messagingTemplate.convertAndSend("/topic/mandi/" + mandiId + "/slots", message);
                    messagingTemplate.convertAndSend("/topic/mandi/" + mandiId + "/queue", message);
                }
                messagingTemplate.convertAndSend("/topic/slots", message);
                log.info("Broadcasted real-time event [{}] for mandi [{}] post-commit", eventType, mandiId);
            } catch (Exception e) {
                log.warn("Failed to broadcast real-time event [{}]: {}", eventType, e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcastAction.run();
                }
            });
        } else {
            broadcastAction.run();
        }
    }
}
