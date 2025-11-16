package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardStatusUpdateRequest;
import com.example.bankcards.dto.card.CardUpdateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.contracts.CardCommandServiceInterface;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.EncryptionUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Slf4j
public class CardCommandService extends AbstractCardService implements CardCommandServiceInterface {

    private final UserRepository userRepository;
    private final CardEventService eventService;
    
    public CardCommandService(CardRepository cardRepository, 
                            EncryptionUtil encryptionUtil, 
                            CardMaskingUtil maskingUtil, 
                            UserRepository userRepository,
                            CardEventService eventService) {
        super(cardRepository, encryptionUtil, maskingUtil);
        this.userRepository = userRepository;
        this.eventService = eventService;
    }

    @Override
    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        log.info("Creating new card for user: {}", request.getUserId());

        // Validate card number
        if (!maskingUtil.isValidCardNumber(request.getCardNumber())) {
            throw new InvalidCardException("Invalid card number");
        }

        // Check for duplicate card number
        String encryptedCardNumber = encryptionUtil.encrypt(request.getCardNumber());
        if (cardRepository.existsByCardNumberEncrypted(encryptedCardNumber)) {
            throw new DuplicateResourceException("Card number already exists");
        }

        // Get user
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new UserNotFoundException(request.getUserId()));

        // Check expiry date
        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new InvalidCardException("Card expiry date is in the past");
        }

        // Create card
        Card card = new Card();
        card.setCardNumberEncrypted(encryptedCardNumber);
        card.setCardHolder(request.getCardHolder());
        card.setExpiryDate(request.getExpiryDate());
        card.setCvvEncrypted(encryptionUtil.encrypt(request.getCvv()));
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);
        card.setUser(user);

        card = cardRepository.save(card);

        // Record event
        eventService.recordCardCreatedEvent(card, user.getId());

        log.info("Card created successfully: {}", card.getId());
        return mapToResponse(card);
    }

    @Override
    @Transactional
    public CardResponse updateCard(Long cardId, CardUpdateRequest request, Long userId) {
        log.info("Updating card: {} by user: {}", cardId, userId);

        Card card = getCardWithAuth(cardId, userId);

        if (request.getCardHolder() != null) {
            card.setCardHolder(request.getCardHolder());
        }

        if (request.getExpiryDate() != null) {
            if (request.getExpiryDate().isBefore(LocalDate.now())) {
                throw new InvalidCardException("Card expiry date is in the past");
            }
            card.setExpiryDate(request.getExpiryDate());
        }

        if (request.getStatus() != null) {
            Card.CardStatus newStatus = Card.CardStatus.valueOf(request.getStatus());
            String oldStatus = card.getStatus().name();
            card.setStatus(newStatus);
            eventService.recordCardStatusChangedEvent(cardId, oldStatus, newStatus.name(), userId, "Manual update");
        }

        card = cardRepository.save(card);

        log.info("Card updated successfully: {}", cardId);
        return mapToResponse(card);
    }

    @Override
    @Transactional
    public CardResponse updateCardStatus(Long cardId, CardStatusUpdateRequest request, Long userId) {
        log.info("Updating card status: {} to {}", cardId, request.getStatus());

        Card card = getCardWithAuth(cardId, userId);

        String oldStatus = card.getStatus().name();
        Card.CardStatus newStatus = Card.CardStatus.valueOf(request.getStatus());

        card.setStatus(newStatus);
        card = cardRepository.save(card);

        // Record event
        eventService.recordCardStatusChangedEvent(cardId, oldStatus, newStatus.name(),
                userId, request.getReason());

        log.info("Card status updated: {} from {} to {}", cardId, oldStatus, newStatus);
        return mapToResponse(card);
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId, Long userId) {
        log.info("Deleting card: {} by user: {}", cardId, userId);

        Card card = getCardWithAuth(cardId, userId);

        // Record event before deletion
        eventService.recordCardDeletedEvent(cardId, userId);

        cardRepository.delete(card);
        log.info("Card deleted: {}", cardId);
    }
}
