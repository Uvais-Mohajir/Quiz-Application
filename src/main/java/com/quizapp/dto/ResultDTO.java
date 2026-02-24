package com.quizapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "Common result DTO for submit and view")
public class ResultDTO {

    @Schema(description = "Exam id")
    private String examId;

    @Schema(description = "Exam title")
    private String examTitle;

    @Schema(description = "Map of questionId to selected answer")
    private Map<String, String> answers;

    @Schema(description = "Final score", example = "8")
    private int score;

    @Schema(description = "Submission status", example = "true")
    private boolean submitted;

    @Schema(description = "Submission timestamp")
    private LocalDateTime submittedAt;

    @Schema(description = "Attempt start timestamp")
    private LocalDateTime attemptStartAt;

    @Schema(description = "Attempt end timestamp")
    private LocalDateTime attemptEndAt;

    @Schema(description = "Status message")
    private String message;
}
