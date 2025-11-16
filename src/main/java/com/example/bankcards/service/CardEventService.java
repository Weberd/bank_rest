package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardEvent;
import com.example.bankcards.repository.CardEventRepository;
import com.example.bankcards.service.contracts.CardEventServiceInterface;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardEventService implements CardEventServiceInterface {

    private final CardEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public void recordCardCreatedEvent(Card card, Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("cardId", card.getId());
        data.put("cardHolder", card.getCardHolder());
        data.put("status", card.getStatus().name());
        data.put("balance", card.getBalance());
        data.put("userId", card.getUser().getId());

        saveEvent(card.getId(), "CARD_CREATED", data, userId);
    }

    @Transactional
    @Override
    public void recordCardStatusChangedEvent(Long cardId, String oldStatus, String newStatus, Long userId, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("cardId", cardId);
        data.put("oldStatus", oldStatus);
        data.put("newStatus", newStatus);
        data.put("reason", reason);

        saveEvent(cardId, "CARD_STATUS_CHANGED", data, userId);
    }

    @Transactional
    @Override
    public void recordCardDeletedEvent(Long cardId, Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("cardId", cardId);
        data.put("deletedAt", LocalDateTime.now());

        saveEvent(cardId, "CARD_DELETED", data, userId);
    }

    private void saveEvent(Long aggregateId, String eventType, Map<String, Object> data,
                           Long userId) {
        try {
            String eventData = objectMapper.writeValueAsString(data);

            CardEvent event = new CardEvent();
            event.setAggregateId(aggregateId);
            event.setEventType(eventType);
            event.setEventData(eventData);
            event.setUserId(userId);
            event.setTimestamp(LocalDateTime.now());

            eventRepository.save(event);
            log.info("Event recorded: {} for aggregate: {}", eventType, aggregateId);

        } catch (JsonProcessingException e) {
            log.error("Error serializing event data", e);
            throw new RuntimeException("Error recording event", e);
        }
    }
}