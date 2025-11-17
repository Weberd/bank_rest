package com.example.bankcards.service.contracts;

import com.example.bankcards.dto.card.CardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Query interface - handles read operations
 */
public interface CardQueryServiceInterface {
    Page<CardResponse> getAllCards(Pageable pageable);
    Page<CardResponse> getUserCards(Long userId, Pageable pageable);
}