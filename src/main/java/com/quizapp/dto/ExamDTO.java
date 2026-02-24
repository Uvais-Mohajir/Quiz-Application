package com.quizapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Common exam DTO")
public class ExamDTO {

    @Schema(description = "Exam id", example = "67b8f9eaf12c5f0012ab34cd")
    private String examId;

    @Schema(description = "Exam title", example = "Banking Awareness Mock 1")
    private String title;

    @Schema(description = "Exam description")
    private String description;

    @Schema(description = "Duration in minutes", example = "60")
    private int durationMinutes;

    @Schema(description = "Start timestamp")
    private LocalDateTime startTime;

    @Schema(description = "End timestamp")
    private LocalDateTime endTime;

    @Schema(description = "Whether exam is active", example = "true")
    private boolean active;
}
