package com.quizapp.dto;

import com.quizapp.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Common user DTO for registration and user details")
public class UserDTO {

    @Schema(description = "User id", example = "67b8f9eaf12c5f0012ab3411")
    private String id;

    @Schema(description = "Full name", example = "Alex Johnson")
    @NotBlank
    private String name;

    @Schema(description = "Email address", example = "alex@example.com")
    @Email
    @NotBlank
    private String email;

    @Schema(description = "Password (used in registration only)", example = "password123")
    @NotBlank
    @Size(min = 6)
    private String password;

    @Schema(description = "Role", example = "PARTICIPANT")
    @NotNull
    private Role role;

    @Schema(description = "Approval status")
    private boolean approved;

    @Schema(description = "Email verified status")
    private boolean emailVerified;

    @Schema(description = "Session/JWT token placeholder", example = "SESSION_AUTH")
    private String token;
}
