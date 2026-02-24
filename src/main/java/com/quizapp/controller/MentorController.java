package com.quizapp.controller;

import com.quizapp.dto.ApiResponse;
import com.quizapp.dto.ExamDTO;
import com.quizapp.dto.QuestionDTO;
import com.quizapp.service.ExamService;
import com.quizapp.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/api/mentor")
@RequiredArgsConstructor
@Tag(name = "Mentor", description = "Mentor APIs for exam and question management")
public class MentorController {

    private final ExamService examService;
    private final QuestionService questionService;

    @PostMapping("/exam")
    @Operation(summary = "Create an exam")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Exam created successfully")
    })
    public ApiResponse<ExamDTO> createExam(
            @RequestBody ExamDTO examDTO,
            Authentication authentication
    ) {
        String mentorEmail = authentication.getName();
        ExamDTO response = examService.createExam(examDTO, mentorEmail);
        return ApiResponse.success("Exam created successfully", response);
    }

    @PostMapping("/question")
    @Operation(summary = "Add a question to exam")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Question added successfully")
    })
    public ApiResponse<Void> addQuestion(
            @RequestBody QuestionDTO questionDTO,
            Authentication authentication
    ) {
        String mentorEmail = authentication.getName();
        questionService.addQuestion(questionDTO, mentorEmail);
        return ApiResponse.success("Question added successfully");
    }

    @GetMapping("/question/template")
    @Operation(summary = "Download Excel template for bulk question upload")
    public ResponseEntity<byte[]> downloadQuestionTemplate() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Questions");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("examId");
            header.createCell(1).setCellValue("questionText");
            header.createCell(2).setCellValue("option1");
            header.createCell(3).setCellValue("option2");
            header.createCell(4).setCellValue("option3");
            header.createCell(5).setCellValue("option4");
            header.createCell(6).setCellValue("correctAnswer");

            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("YOUR_EXAM_ID");
            sample.createCell(1).setCellValue("What is 2 + 2?");
            sample.createCell(2).setCellValue("3");
            sample.createCell(3).setCellValue("4");
            sample.createCell(4).setCellValue("5");
            sample.createCell(5).setCellValue("6");
            sample.createCell(6).setCellValue("4");

            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=question-template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    @PostMapping(value = "/question/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Excel and bulk insert questions")
    public ApiResponse<Integer> uploadQuestionExcel(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        int inserted = questionService.addQuestionsFromExcel(file, authentication.getName());
        return ApiResponse.success("Questions uploaded successfully", inserted);
    }
}
