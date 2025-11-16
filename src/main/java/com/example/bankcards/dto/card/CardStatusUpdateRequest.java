package com.example.bankcards.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardStatusUpdateRequest {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|BLOCKED|EXPIRED", message = "Invalid status")
    private String status;

    private String reason;
}