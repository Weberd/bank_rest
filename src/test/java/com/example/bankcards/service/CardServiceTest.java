package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardStatusUpdateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.InvalidCardException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class CardServiceTest {

    @MockBean
    private CardRepository cardRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private EncryptionUtil encryptionUtil;

    @MockBean
    private CardMaskingUtil maskingUtil;

    @MockBean
    private CardEventService eventService;

    @Autowired
    private CardCommandService cardCommandService;

    @Autowired
    private CardQueryService cardQueryService;

    private User testUser;
    private Card testCard;
    private CardCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.USER);

        testCard = new Card();
        testCard.setId(1L);
        testCard.setCardNumberEncrypted("encrypted123");
        testCard.setCardHolder("John Doe");
        testCard.setExpiryDate(LocalDate.now().plusYears(3));
        testCard.setStatus(Card.CardStatus.ACTIVE);
        testCard.setBalance(BigDecimal.valueOf(1000));
        testCard.setUser(testUser);

        createRequest = new CardCreateRequest();
        createRequest.setCardNumber("1234567890123456");
        createRequest.setCardHolder("John Doe");
        createRequest.setExpiryDate(LocalDate.now().plusYears(3));
        createRequest.setCvv("123");
        createRequest.setUserId(1L);
        createRequest.setInitialBalance(BigDecimal.valueOf(1000));
    }

    @Test
    void createCard_Success() {
        when(maskingUtil.isValidCardNumber(anyString())).thenReturn(true);
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted");
        when(cardRepository.existsByCardNumberEncrypted(anyString())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(encryptionUtil.decrypt(anyString())).thenReturn("1234567890123456");
        when(maskingUtil.maskCardNumber(anyString())).thenReturn("**** **** **** 3456");

        CardResponse response = cardCommandService.createCard(createRequest);

        assertNotNull(response);
        assertEquals("**** **** **** 3456", response.getCardNumberMasked());
        verify(cardRepository, times(1)).save(any(Card.class));
        verify(eventService, times(1)).recordCardCreatedEvent(any(Card.class), eq(1L));
    }

    @Test
    void createCard_InvalidCardNumber() {
        assertThrows(InvalidCardException.class, () -> {
            cardCommandService.createCard(createRequest);
        });

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void createCard_DuplicateCardNumber() {
        when(maskingUtil.isValidCardNumber(anyString())).thenReturn(true);
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted");
        when(cardRepository.existsByCardNumberEncrypted(anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            cardCommandService.createCard(createRequest);
        });

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void createCard_ExpiredDate() {
        createRequest.setExpiryDate(LocalDate.now().minusDays(1));

        assertThrows(InvalidCardException.class, () -> {
            cardCommandService.createCard(createRequest);
        });
    }

    @Test
    void updateCardStatus_Success() {
        CardStatusUpdateRequest request = new CardStatusUpdateRequest("BLOCKED", "Lost card");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(encryptionUtil.decrypt(anyString())).thenReturn("1234567890123456");
        when(maskingUtil.maskCardNumber(anyString())).thenReturn("**** **** **** 3456");

        CardResponse response = cardCommandService.updateCardStatus(1L, request, 1L);

        assertNotNull(response);
        verify(eventService, times(1)).recordCardStatusChangedEvent(
                eq(1L), eq("ACTIVE"), eq("BLOCKED"), eq(1L), eq("Lost card"));
    }

    @Test
    void updateCardStatus_UnauthorizedUser() {
        CardStatusUpdateRequest request = new CardStatusUpdateRequest("BLOCKED", "Test");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThrows(UnauthorizedException.class, () -> {
            cardCommandService.updateCardStatus(1L, request, 999L);
        });
    }

    @Test
    void deleteCard_Success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        cardCommandService.deleteCard(1L, 1L);

        verify(eventService, times(1)).recordCardDeletedEvent(1L, 1L);
        verify(cardRepository, times(1)).delete(testCard);
    }
}