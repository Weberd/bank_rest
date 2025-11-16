package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class TransferServiceTest {

    @MockBean
    private TransferRepository transferRepository;

    @MockBean
    private CardRepository cardRepository;

    @MockBean
    private EncryptionUtil encryptionUtil;

    @MockBean
    private CardMaskingUtil maskingUtil;

    @MockBean
    private CardEventService eventService;

    @Autowired
    private TransferService transferService;

    private Card fromCard;
    private Card toCard;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setCardNumberEncrypted("encrypted1");
        fromCard.setBalance(BigDecimal.valueOf(1000));
        fromCard.setStatus(Card.CardStatus.ACTIVE);
        fromCard.setExpiryDate(LocalDate.now().plusYears(3));
        fromCard.setUser(testUser);

        toCard = new Card();
        toCard.setId(2L);
        toCard.setCardNumberEncrypted("encrypted2");
        toCard.setBalance(BigDecimal.valueOf(500));
        toCard.setStatus(Card.CardStatus.ACTIVE);
        toCard.setExpiryDate(LocalDate.now().plusYears(3));
        toCard.setUser(testUser);

        transferRequest = new TransferRequest();
        transferRequest.setFromCardId(1L);
        transferRequest.setToCardId(2L);
        transferRequest.setAmount(BigDecimal.valueOf(100));
        transferRequest.setDescription("Test transfer");
    }

    @Test
    @Transactional
    void executeTransfer_Success() {
        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(i -> i.getArguments()[0]);

        Transfer savedTransfer = new Transfer();
        savedTransfer.setId(1L);
        savedTransfer.setFromCard(fromCard);
        savedTransfer.setToCard(toCard);
        savedTransfer.setAmount(BigDecimal.valueOf(100));
        savedTransfer.setStatus(Transfer.TransferStatus.COMPLETED);
        savedTransfer.setUserId(1L);

        when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer);
        when(encryptionUtil.decrypt(anyString())).thenReturn("1234567890123456");
        when(maskingUtil.maskCardNumber(anyString())).thenReturn("**** **** **** 3456");

        TransferResponse response = transferService.executeTransfer(transferRequest, 1L);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(100), response.getAmount());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(BigDecimal.valueOf(900), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(600), toCard.getBalance());

        verify(transferRepository, times(1)).save(any(Transfer.class));
    }

    @Test
    void executeTransfer_SameCard() {
        transferRequest.setToCardId(1L);

        assertThrows(InvalidTransferException.class, () -> {
            transferService.executeTransfer(transferRequest, 1L);
        });

        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    void executeTransfer_NegativeAmount() {
        transferRequest.setAmount(BigDecimal.valueOf(-100));

        assertThrows(InvalidTransferException.class, () -> {
            transferService.executeTransfer(transferRequest, 1L);
        });
    }

    @Test
    void executeTransfer_InsufficientBalance() {
        transferRequest.setAmount(BigDecimal.valueOf(2000));

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InsufficientBalanceException.class, () -> {
            transferService.executeTransfer(transferRequest, 1L);
        });

        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    void executeTransfer_FromCardNotActive() {
        fromCard.setStatus(Card.CardStatus.BLOCKED);

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        assertThrows(CardNotActiveException.class, () -> {
            transferService.executeTransfer(transferRequest, 1L);
        });
    }

    @Test
    void executeTransfer_ToCardNotActive() {
        toCard.setStatus(Card.CardStatus.BLOCKED);

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        assertThrows(CardNotActiveException.class, () -> {
            transferService.executeTransfer(transferRequest, 1L);
        });
    }

    @Test
    void executeTransfer_UnauthorizedUser() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        toCard.setUser(anotherUser);

        when(cardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toCard));

        assertThrows(UnauthorizedException.class, () -> {
            transferService.executeTransfer(transferRequest, 1L);
        });
    }

    @Test
    void executeTransfer_CardNotFound() {
        assertThrows(CardNotFoundException.class, () -> {
            transferService.executeTransfer(transferRequest, 1L);
        });
    }
}