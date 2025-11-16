package com.example.bankcards.service.contracts;

import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.dto.user.UserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface UserServiceInterface {
    @Transactional(readOnly = true)
    UserResponse getUserById(Long userId);

    @Transactional(readOnly = true)
    UserResponse getUserByUsername(String username);

    @Transactional(readOnly = true)
    Page<UserResponse> getAllUsers(Pageable pageable);

    @Transactional
    UserResponse updateUser(Long userId, UserUpdateRequest request);

    @Transactional
    void deleteUser(Long userId);

    @Transactional
    UserResponse toggleUserStatus(Long userId);
}
