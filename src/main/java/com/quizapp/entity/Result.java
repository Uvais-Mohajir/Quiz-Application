package com.quizapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result {

    @Id
    private String id;

    private String examId;
    private String studentId;

    private int score;

    private boolean submitted;

    private Map<String, String> answers;

    private LocalDateTime attemptStartAt;
    private LocalDateTime attemptEndAt;

    private LocalDateTime submittedAt;

    private boolean dayReminderSent;

    private boolean hourReminderSent;
}
