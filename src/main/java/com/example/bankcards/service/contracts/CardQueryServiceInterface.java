package com.example.bankcards.service.contracts;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Query interface - handles read operations
 */
public interface CardQueryServiceInterface {
    CardResponse getCardById(Long cardId, Long userId);
    Page<CardResponse> getAllCards(Pageable pageable);
    Page<CardResponse> getUserCards(Long userId, Pageable pageable);
    Page<CardResponse> getCardsByStatus(Card.CardStatus status, Pageable pageable);
    Page<CardResponse> getUserCardsByStatus(Long userId, Card.CardStatus status, Pageable pageable);
}