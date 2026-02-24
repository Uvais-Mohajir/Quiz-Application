package com.quizapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "exams")
public class Exam {

    @Id
    private String id;

    private String title;

    private String description;

    private int durationMinutes;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private boolean active;

    // Mentor who created the exam
    private String mentorId;
}
