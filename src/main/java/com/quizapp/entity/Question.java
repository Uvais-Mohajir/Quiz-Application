package com.quizapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "questions")
public class Question {

    @Id
    private String id;

    private String examId;

    private String questionText;

    private List<String> options;

    // store correct answer internally (not sent to student)
    private String correctAnswer;
}
