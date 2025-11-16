package com.example.bankcards.dto.user;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    @Email(message = "Invalid email format")
    private String email;

    private String firstName;
    private String lastName;
}