package com.example.bankcards.service.contracts;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import org.springframework.transaction.annotation.Transactional;

public interface AuthServiceInterface {
    @Transactional
    AuthResponse register(RegisterRequest request);

    @Transactional(readOnly = true)
    AuthResponse login(LoginRequest request);
}
