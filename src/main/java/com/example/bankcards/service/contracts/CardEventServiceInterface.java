package com.example.bankcards.service.contracts;

import com.example.bankcards.entity.Card;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface CardEventServiceInterface {
    @Transactional
    void recordCardCreatedEvent(Card card, Long userId);

    @Transactional
    void recordCardStatusChangedEvent(Long cardId, String oldStatus, String newStatus, Long userId, String reason);

    @Transactional
    void recordCardDeletedEvent(Long cardId, Long userId);
}
