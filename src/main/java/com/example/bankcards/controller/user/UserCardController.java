package com.example.bankcards.controller.user;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardStatusUpdateRequest;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.service.contracts.CardCommandServiceInterface;
import com.example.bankcards.service.contracts.CardQueryServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/user/cards")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Cards", description = "User card management endpoints")
public class UserCardController {

    private final CardCommandServiceInterface cardCommandService;
    private final CardQueryServiceInterface cardQueryService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get current user's cards")
    public ResponseEntity<Page<CardResponse>> getMyCards(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = getUserId(authentication);
        Page<CardResponse> cards = cardQueryService.getUserCards(userId, pageable);
        return ResponseEntity.ok(cards);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Request card block")
    public ResponseEntity<CardResponse> blockCard(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = getUserId(authentication);
        CardStatusUpdateRequest request = new CardStatusUpdateRequest("BLOCKED", "User requested block");
        CardResponse response = cardCommandService.updateCardStatus(id, request, userId);
        return ResponseEntity.ok(response);
    }

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userDetailsService.loadUserEntityByUsername(username).getId();
    }
}