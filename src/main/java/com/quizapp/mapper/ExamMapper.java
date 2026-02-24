package com.quizapp.mapper;

import com.quizapp.dto.ExamDTO;
import com.quizapp.entity.Exam;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {

    public Exam toEntity(ExamDTO dto, String mentorId) {
        if (dto == null) {
            return null;
        }
        return Exam.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .durationMinutes(dto.getDurationMinutes())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .active(true)
                .mentorId(mentorId)
                .build();
    }

    public ExamDTO toDTO(Exam exam) {
        if (exam == null) {
            return null;
        }
        ExamDTO dto = new ExamDTO();
        dto.setExamId(exam.getId());
        dto.setTitle(exam.getTitle());
        dto.setDescription(exam.getDescription());
        dto.setDurationMinutes(exam.getDurationMinutes());
        dto.setStartTime(exam.getStartTime());
        dto.setEndTime(exam.getEndTime());
        dto.setActive(exam.isActive());
        return dto;
    }
}
