package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.service.contracts.CardQueryServiceInterface;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;

@Service
@Slf4j
public class CardQueryService extends AbstractCardService implements CardQueryServiceInterface {

    public CardQueryService(CardRepository cardRepository, EncryptionUtil encryptionUtil, CardMaskingUtil maskingUtil) {
        super(cardRepository, encryptionUtil, maskingUtil);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCardById(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        return mapToResponse(card);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getUserCards(Long userId, Pageable pageable) {
        return cardRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsByStatus(Card.CardStatus status, Pageable pageable) {
        return cardRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getUserCardsByStatus(Long userId, Card.CardStatus status, Pageable pageable) {
        return cardRepository.findByUserIdAndStatus(userId, status, pageable).map(this::mapToResponse);
    }
}
