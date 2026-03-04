package com.neo.nexora.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirm {
    private String token;
    private String newPassword;
    private String confirmPassword;
}
