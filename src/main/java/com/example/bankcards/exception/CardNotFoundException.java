package com.example.bankcards.exception;

public class CardNotFoundException extends ResourceNotFoundException {
    public CardNotFoundException(Long cardId) {
        super("Card not found with ID: " + cardId);
    }
}