package com.quizapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Common question DTO")
public class QuestionDTO {

    @Schema(description = "Question id", example = "67b8f9eaf12c5f0012ab34ef")
    private String questionId;

    @Schema(description = "Exam id", example = "67b8f9eaf12c5f0012ab34cd")
    private String examId;

    @Schema(description = "Question text")
    private String questionText;

    @Schema(description = "Available options")
    private List<String> options;

    @Schema(description = "Correct option value (used while creating question)", example = "A")
    private String correctAnswer;
}
