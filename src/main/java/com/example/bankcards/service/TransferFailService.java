package com.example.bankcards.service;

import com.example.bankcards.entity.Transfer;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.service.contracts.TransferFailServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferFailService implements TransferFailServiceInterface {

    private final TransferRepository transferRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void logFailure(Transfer transfer, String message) {

        try {
            transfer.setStatus(Transfer.TransferStatus.FAILED);
            transfer.setDescription(
                (transfer.getDescription()  == null ? "" : transfer.getDescription()) + " | failed: " + message
            );

            transferRepository.save(transfer);

            log.warn("Transfer {} marked FAILED", transfer.getId());
        } catch (Exception loggingEx) {
            // Never break main flow
            log.error("Failed to persist FAILED transfer {}", transfer.getId(), loggingEx);
        }
    }
}
