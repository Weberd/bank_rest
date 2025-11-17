package com.example.bankcards.controller.admin;

import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.dto.user.UserUpdateRequest;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.service.contracts.UserServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Users", description = "Admin user management endpoints")
public class AdminUserController {

    private final UserServiceInterface userService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user (Admin only)")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {

        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user (Admin only)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle user enabled/disabled status (Admin only)")
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable Long id) {
        UserResponse response = userService.toggleUserStatus(id);
        return ResponseEntity.ok(response);
    }

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userDetailsService.loadUserEntityByUsername(username).getId();
    }
}