package com.quizapp.service.impl;

import com.quizapp.dto.QuestionDTO;
import com.quizapp.entity.Exam;
import com.quizapp.entity.Question;
import com.quizapp.entity.User;
import com.quizapp.exception.BadRequestException;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.mapper.QuestionMapper;
import com.quizapp.repository.ExamRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final QuestionMapper questionMapper;

    @Override
    public void addQuestion(QuestionDTO questionDTO, String creatorEmail) {

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!creator.isApproved()) {
            throw new BadRequestException("Account is blocked");
        }

        Exam exam = examRepository.findById(questionDTO.getExamId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        if (!creator.getId().equals(exam.getMentorId())) {
            throw new BadRequestException("You can only modify your own exams");
        }
        if (exam.getStartTime() != null && !LocalDateTime.now().isBefore(exam.getStartTime())) {
            throw new BadRequestException("Question editing is closed after exam start time");
        }

        Question question = questionMapper.toEntity(questionDTO);
        question.setExamId(exam.getId());

        questionRepository.save(question);
    }

    @Override
    public int addQuestionsFromExcel(MultipartFile file, String creatorEmail) {
        return addQuestionsFromExcel(file, creatorEmail, null);
    }

    @Override
    public int addQuestionsFromExcel(MultipartFile file, String creatorEmail, String fixedExamId) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!creator.isApproved()) {
            throw new BadRequestException("Account is blocked");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Excel file is required");
        }

        int inserted = 0;
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String examId = fixedExamId != null && !fixedExamId.isBlank()
                        ? fixedExamId
                        : getCellAsString(row, 0);
                String questionText = getCellAsString(row, 1);
                String option1 = getCellAsString(row, 2);
                String option2 = getCellAsString(row, 3);
                String option3 = getCellAsString(row, 4);
                String option4 = getCellAsString(row, 5);
                String correctAnswer = getCellAsString(row, 6);

                if (examId.isBlank() || questionText.isBlank() || correctAnswer.isBlank()) {
                    continue;
                }

                Exam exam = examRepository.findById(examId)
                        .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
                if (!creator.getId().equals(exam.getMentorId())) {
                    throw new BadRequestException("You can only modify your own exams");
                }
                if (exam.getStartTime() != null && !LocalDateTime.now().isBefore(exam.getStartTime())) {
                    throw new BadRequestException("Question editing is closed after exam start time");
                }

                List<String> options = new ArrayList<>();
                if (!option1.isBlank()) options.add(option1);
                if (!option2.isBlank()) options.add(option2);
                if (!option3.isBlank()) options.add(option3);
                if (!option4.isBlank()) options.add(option4);

                if (options.isEmpty()) {
                    continue;
                }

                QuestionDTO dto = new QuestionDTO();
                dto.setExamId(exam.getId());
                dto.setQuestionText(questionText);
                dto.setOptions(options);
                dto.setCorrectAnswer(correctAnswer);

                Question question = questionMapper.toEntity(dto);
                question.setExamId(exam.getId());
                questionRepository.save(question);
                inserted++;
            }
            return inserted;
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Invalid Excel format");
        }
    }

    @Override
    public List<QuestionDTO> getQuestionsByExam(String examId) {

        List<Question> questions = questionRepository.findByExamId(examId);

        return questions.stream()
                .map(q -> questionMapper.toDTO(q, false))
                .toList();
    }

    private String getCellAsString(Row row, int index) {
        if (row.getCell(index) == null) {
            return "";
        }
        return row.getCell(index).toString().trim();
    }
}
