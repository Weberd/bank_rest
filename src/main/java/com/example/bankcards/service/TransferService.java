package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.service.contracts.CardEventServiceInterface;
import com.example.bankcards.service.contracts.TransferServiceInterface;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService implements TransferServiceInterface {

    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final EncryptionUtil encryptionUtil;
    private final CardMaskingUtil maskingUtil;
    @Autowired
    private final CardEventServiceInterface eventService;
    @Autowired
    private final TransferFailService failService;

    @Transactional
    @Override
    public TransferResponse executeTransfer(TransferRequest request, Long userId) {
        log.info("Executing transfer from card {} to card {} for user {}",
            request.getFromCardId(), request.getToCardId(), userId);

        validateTransferRequest(request, userId);

        Card fromCard = cardRepository.findByIdForUpdate(request.getFromCardId())
            .orElseThrow(() -> new CardNotFoundException(request.getFromCardId()));

        Card toCard = cardRepository.findByIdForUpdate(request.getToCardId())
            .orElseThrow(() -> new CardNotFoundException(request.getToCardId()));

        validateCards(fromCard, toCard, request.getAmount(), userId);

        Transfer transfer = new Transfer();
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(request.getAmount());
        transfer.setDescription(request.getDescription());
        transfer.setUserId(userId);

        try {
            fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
            toCard.setBalance(toCard.getBalance().add(request.getAmount()));
            // fromCard and toCard save are not needed because Spring Data flush while commit

            transfer.setStatus(Transfer.TransferStatus.COMPLETED);
            transferRepository.save(transfer);

            log.info("Transfer completed successfully: {}", transfer.getId());
            return mapToResponse(transfer);
        } catch (Exception ex) {
            log.error("Transfer {} failed: {}", transfer.getId(), ex.getMessage(), ex);
            failService.logFailure(transfer, ex.getMessage());

            throw ex;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TransferResponse> getUserTransfers(Long userId, Pageable pageable) {
        return transferRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TransferResponse> getCardTransfers(Long cardId, Long userId, Pageable pageable) {
        return transferRepository.findByCardIdAndUserId(cardId, userId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public TransferResponse getTransferById(Long transferId, Long userId) {
        Transfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));

        if (!transfer.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this transfer");
        }

        return mapToResponse(transfer);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedTransfer(Transfer transfer) {
        transferRepository.save(transfer);
    }

    private void validateTransferRequest(TransferRequest request, Long userId) {
        if (request.getFromCardId().equals(request.getToCardId())) {
            throw new InvalidTransferException("Cannot transfer to the same card");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be greater than zero");
        }
    }

    private void validateCards(Card fromCard, Card toCard, BigDecimal amount, Long userId) {
        // Verify ownership
        if (!fromCard.getUser().getId().equals(userId) || !toCard.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only transfer between your own cards");
        }

        // Verify cards are active
        if (!fromCard.isActive()) {
            throw new CardNotActiveException("Source card is not active. Status: " + fromCard.getStatus());
        }

        if (!toCard.isActive()) {
            throw new CardNotActiveException("Destination card is not active. Status: " + toCard.getStatus());
        }

        // Verify sufficient balance
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Available: %s, Required: %s", fromCard.getBalance(), amount)
            );
        }
    }

    private TransferResponse mapToResponse(Transfer transfer) {
        String fromCardNumber = encryptionUtil.decrypt(transfer.getFromCard().getCardNumberEncrypted());
        String toCardNumber = encryptionUtil.decrypt(transfer.getToCard().getCardNumberEncrypted());

        return new TransferResponse(
            transfer.getId(),
            transfer.getFromCard().getId(),
            maskingUtil.maskCardNumber(fromCardNumber),
            transfer.getToCard().getId(),
            maskingUtil.maskCardNumber(toCardNumber),
            transfer.getAmount(),
            transfer.getStatus().name(),
            transfer.getDescription(),
            transfer.getUserId(),
            transfer.getCreatedAt()
        );
    }
}