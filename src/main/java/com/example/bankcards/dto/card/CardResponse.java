package com.example.bankcards.dto.card;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private String cardNumberMasked;
    private String cardHolder;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;
    private String status;
    private BigDecimal balance;
    private Long userId;
    private String userName;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}