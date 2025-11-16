package com.example.bankcards.service.contracts;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface TransferServiceInterface {
    @Transactional
    TransferResponse executeTransfer(TransferRequest request, Long userId);

    @Transactional(readOnly = true)
    Page<TransferResponse> getUserTransfers(Long userId, Pageable pageable);

    @Transactional(readOnly = true)
    Page<TransferResponse> getCardTransfers(Long cardId, Long userId, Pageable pageable);

    @Transactional(readOnly = true)
    TransferResponse getTransferById(Long transferId, Long userId);
}
