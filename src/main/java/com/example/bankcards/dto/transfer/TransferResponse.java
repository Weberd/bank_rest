package com.example.bankcards.dto.transfer;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private Long id;
    private Long fromCardId;
    private String fromCardNumberMasked;
    private Long toCardId;
    private String toCardNumberMasked;
    private BigDecimal amount;
    private String status;
    private String description;
    private Long userId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
