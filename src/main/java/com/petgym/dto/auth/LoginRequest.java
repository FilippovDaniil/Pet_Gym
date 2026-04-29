package com.petgym.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// DTO для запроса на вход в систему
@Data
public class LoginRequest {

    @NotBlank
    @Email
    private String email; // логин — это email

    @NotBlank
    private String password; // пароль в открытом виде (HTTPS шифрует при передаче)
}
