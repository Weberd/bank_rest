package com.example.bankcards.exception;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }
}
