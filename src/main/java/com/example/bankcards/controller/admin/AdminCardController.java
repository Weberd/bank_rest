package com.example.bankcards.controller.admin;

import com.example.bankcards.dto.PageResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardStatusUpdateRequest;
import com.example.bankcards.dto.card.CardUpdateRequest;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.service.contracts.CardCommandServiceInterface;
import com.example.bankcards.service.contracts.CardQueryServiceInterface;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/cards")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Cards", description = "Admin card management endpoints")
public class AdminCardController {

    private final CardCommandServiceInterface cardCommandService;
    private final CardQueryServiceInterface cardQueryService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new card (Admin only)")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardCreateRequest request) {
        CardResponse response = cardCommandService.createCard(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all cards (Admin only)")
    public ResponseEntity<PageResponse<CardResponse>> getAllCards(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<CardResponse> cards = cardQueryService.getAllCards(pageable);
        return ResponseEntity.ok(new PageResponse<>(cards));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update card")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CardUpdateRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = getUserId(authentication);
        CardResponse response = cardCommandService.updateCard(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update card status")
    public ResponseEntity<CardResponse> updateCardStatus(
            @PathVariable Long id,
            @Valid @RequestBody CardStatusUpdateRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = getUserId(authentication);
        CardResponse response = cardCommandService.updateCardStatus(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete card (Admin only)")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = getUserId(authentication);
        cardCommandService.deleteCard(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userDetailsService.loadUserEntityByUsername(username).getId();
    }
}