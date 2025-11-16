package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.service.contracts.TransferServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Transfers", description = "Transfer management endpoints")
public class TransferController {

    private final TransferServiceInterface transferService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Execute transfer between own cards")
    public ResponseEntity<TransferResponse> executeTransfer(
            @Valid @RequestBody TransferRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = getUserId(authentication);
        TransferResponse response = transferService.executeTransfer(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get current user's transfers")
    public ResponseEntity<Page<TransferResponse>> getMyTransfers(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = getUserId(authentication);
        Page<TransferResponse> transfers = transferService.getUserTransfers(userId, pageable);
        return ResponseEntity.ok(transfers);
    }

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userDetailsService.loadUserEntityByUsername(username).getId();
    }
}