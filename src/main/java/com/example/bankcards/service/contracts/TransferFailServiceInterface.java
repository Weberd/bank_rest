package com.example.bankcards.service.contracts;

import com.example.bankcards.entity.Transfer;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface TransferFailServiceInterface {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void logFailure(Transfer transfer, String message);
}
