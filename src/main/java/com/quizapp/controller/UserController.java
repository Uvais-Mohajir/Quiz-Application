package com.quizapp.controller;

import com.quizapp.dto.ApiResponse;
import com.quizapp.dto.ExamDTO;
import com.quizapp.dto.QuestionDTO;
import com.quizapp.dto.ResultDTO;
import com.quizapp.entity.Exam;
import com.quizapp.entity.Result;
import com.quizapp.entity.User;
import com.quizapp.exception.BadRequestException;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.repository.ExamRepository;
import com.quizapp.repository.ResultRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.ExamService;
import com.quizapp.service.QuestionService;
import com.quizapp.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "Participant APIs for exam attempt workflow")
public class UserController {

    private final StudentService studentService;
    private final ExamService examService;
    private final QuestionService questionService;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ResultRepository resultRepository;

    @GetMapping("/exam/active")
    @Operation(summary = "Get all active exams")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active exams fetched")
    })
    public ApiResponse<List<ExamDTO>> getActiveExams() {
        return ApiResponse.success("Active exams fetched", examService.getActiveExams());
    }

    @GetMapping("/question/exam/{examId}")
    @Operation(summary = "Get questions by exam id")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Questions fetched successfully")
    })
    public ApiResponse<List<QuestionDTO>> getQuestions(
            @Parameter(description = "Exam id") @PathVariable String examId,
            Authentication authentication
    ) {
        User student = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        Result registration = resultRepository.findByExamIdAndStudentId(examId, student.getId())
                .orElseThrow(() -> new BadRequestException("Please register before viewing questions"));
        if (registration.isSubmitted()) {
            throw new BadRequestException("Exam already submitted");
        }

        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BadRequestException("Exam has not started yet");
        }
        if (exam.getEndTime() != null && !now.isBefore(exam.getEndTime())) {
            throw new BadRequestException("Exam window is closed");
        }

        return ApiResponse.success("Questions fetched successfully", questionService.getQuestionsByExam(examId));
    }

    @PostMapping("/register-exam/{examId}")
    @Operation(summary = "Register current user for an exam")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registered for exam successfully")
    })
    public ApiResponse<Void> registerForExam(
            @Parameter(description = "Exam id") @PathVariable String examId,
            Authentication authentication
    ) {
        studentService.registerForExam(examId, authentication.getName());
        return ApiResponse.success("Registered for exam successfully");
    }

    @PostMapping("/submit-exam")
    @Operation(summary = "Submit exam answers")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Exam submitted successfully")
    })
    public ResultDTO submitExam(
            @RequestBody ResultDTO resultDTO,
            Authentication authentication
    ) {
        return studentService.submitExam(resultDTO, authentication.getName());
    }

    @GetMapping("/results")
    @Operation(summary = "Get current user results")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Results fetched successfully")
    })
    public ApiResponse<List<ResultDTO>> getMyResults(Authentication authentication) {
        return ApiResponse.success(
                "Results fetched successfully",
                studentService.getMyResults(authentication.getName())
        );
    }
}
