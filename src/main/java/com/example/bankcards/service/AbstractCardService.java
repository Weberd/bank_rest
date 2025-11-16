package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
abstract public class AbstractCardService {
    protected final CardRepository cardRepository;
    protected final EncryptionUtil encryptionUtil;
    protected final CardMaskingUtil maskingUtil;

    protected Card getCardWithAuth(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        if (!card.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this card");
        }

        return card;
    }

    protected CardResponse mapToResponse(Card card) {
        String decryptedCardNumber = encryptionUtil.decrypt(card.getCardNumberEncrypted());
        String maskedCardNumber = maskingUtil.maskCardNumber(decryptedCardNumber);

        return new CardResponse(
                card.getId(),
                maskedCardNumber,
                card.getCardHolder(),
                card.getExpiryDate(),
                card.getStatus().name(),
                card.getBalance(),
                card.getUser().getId(),
                card.getUser().getUsername(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}
