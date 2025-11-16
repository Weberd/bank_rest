package com.example.bankcards.service.contracts;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardStatusUpdateRequest;
import com.example.bankcards.dto.card.CardUpdateRequest;

/**
 * Command interface - handles write operations
 */
public interface CardCommandServiceInterface {
    CardResponse createCard(CardCreateRequest request);
    CardResponse updateCard(Long cardId, CardUpdateRequest request, Long userId);
    CardResponse updateCardStatus(Long cardId, CardStatusUpdateRequest request, Long userId);
    void deleteCard(Long cardId, Long userId);
}