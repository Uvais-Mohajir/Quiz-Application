package com.quizapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Common login DTO")
public class LoginDTO {

    @Schema(description = "Registered email", example = "user@example.com")
    @Email
    @NotBlank
    private String email;

    @Schema(description = "Account password", example = "secret123")
    @NotBlank
    private String password;
}
