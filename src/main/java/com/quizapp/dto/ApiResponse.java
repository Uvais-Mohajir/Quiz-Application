package com.quizapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "ApiResponse", description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Operation status")
    private boolean success;
    @Schema(description = "User friendly message")
    private String message;
    @Schema(description = "Response payload")
    private T data;

    // ===============================
    // SUCCESS (WITH DATA)
    // ===============================
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    // ===============================
    // SUCCESS (NO DATA)
    // ===============================
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    // ===============================
    // ERROR
    // ===============================
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
