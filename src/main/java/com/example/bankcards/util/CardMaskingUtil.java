package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CardMaskingUtil {

    @Value("${app.card.mask-pattern}")
    private String maskPattern;

    @Value("${app.card.visible-digits}")
    private int visibleDigits;

    /**
     * Masks a card number showing only the last N digits
     * Example: 1234567890123456 -> **** **** **** 3456
     */
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < visibleDigits) {
            return "****";
        }

        String lastDigits = cardNumber.substring(cardNumber.length() - visibleDigits);
        return String.format(maskPattern, lastDigits);
    }

    /**
     * Formats card number with spaces
     * Example: 1234567890123456 -> 1234 5678 9012 3456
     */
    public String formatCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            return cardNumber;
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < cardNumber.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(cardNumber.charAt(i));
        }
        return formatted.toString();
    }

    /**
     * Validates card number using Luhn algorithm
     */
    public boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
            return false;
        }

        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}