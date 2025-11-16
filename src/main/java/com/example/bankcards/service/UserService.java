package com.example.bankcards.service;

import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.dto.user.UserUpdateRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.contracts.UserServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserServiceInterface {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    @Override
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.getEmail() != null) {
            if (userRepository.existsByEmail(request.getEmail()) &&
                !user.getEmail().equals(request.getEmail())) {
                throw new DuplicateResourceException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        user = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        return mapToResponse(user);
    }

    @Transactional
    @Override
    public void deleteUser(Long userId) {
        log.info("Deleting user: {}", userId);

        User user = userRepository.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));

        userRepository.delete(user);
        log.info("User deleted: {}", userId);
    }

    @Transactional
    @Override
    public UserResponse toggleUserStatus(Long userId) {
        log.info("Toggling user status: {}", userId);

        User user = userRepository.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));

        user.setEnabled(!user.getEnabled());
        user = userRepository.save(user);

        log.info("User status toggled to {} for user: {}", user.getEnabled(), userId);
        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole().name(),
            user.getEnabled(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}