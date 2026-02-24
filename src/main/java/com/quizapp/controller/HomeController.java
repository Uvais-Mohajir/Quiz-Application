package com.quizapp.controller;

import com.quizapp.dto.ExamDTO;
import com.quizapp.dto.QuestionDTO;
import com.quizapp.dto.ResultDTO;
import com.quizapp.dto.UserDTO;
import com.quizapp.entity.Exam;
import com.quizapp.entity.Result;
import com.quizapp.entity.Role;
import com.quizapp.entity.User;
import com.quizapp.exception.BadRequestException;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.repository.ExamRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.ResultRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AdminService;
import com.quizapp.service.AuthService;
import com.quizapp.service.ExamService;
import com.quizapp.service.QuestionService;
import com.quizapp.service.StudentService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Hidden
public class HomeController {

    private final AuthService authService;
    private final AdminService adminService;
    private final ExamService examService;
    private final QuestionService questionService;
    private final StudentService studentService;
    private final ResultRepository resultRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;

    @GetMapping({"/", "/index"})
    public String indexPage(Model model) {
        var activeExams = examService.getActiveExams();
        model.addAttribute("liveTestCount", activeExams.size());
        model.addAttribute("activeExams", activeExams);
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Role role,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (role == Role.ADMIN) {
                throw new BadRequestException("Admin self-registration is not allowed");
            }

            UserDTO userDTO = new UserDTO();
            userDTO.setName(name);
            userDTO.setEmail(email);
            userDTO.setPassword(password);
            userDTO.setRole(role);
            authService.register(userDTO);

            redirectAttributes.addAttribute("email", email);
            redirectAttributes.addAttribute("message", "Registration successful. Verify your email using OTP.");
            return "redirect:/verify-otp";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("name", name);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("role", role.name());
            return "redirect:/register";
        }
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "verify-otp";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "forgot-password";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam String email,
            @RequestParam String otp,
            RedirectAttributes redirectAttributes
    ) {
        try {
            authService.verifyRegistrationOtp(email, otp);
            redirectAttributes.addAttribute("message", "Email verified successfully. Please login.");
            return "redirect:/login";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("email", email);
            return "redirect:/verify-otp";
        }
    }

    @PostMapping("/verify-otp/resend")
    public String resendOtp(
            @RequestParam String email,
            RedirectAttributes redirectAttributes
    ) {
        try {
            authService.resendRegistrationOtp(email);
            redirectAttributes.addFlashAttribute("message", "OTP resent successfully");
            redirectAttributes.addAttribute("email", email);
            return "redirect:/verify-otp";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("email", email);
            return "redirect:/verify-otp";
        }
    }

    @PostMapping("/forgot-password/send-otp")
    public String sendForgotPasswordOtp(
            @RequestParam String email,
            RedirectAttributes redirectAttributes
    ) {
        try {
            authService.sendPasswordResetOtp(email);
            redirectAttributes.addFlashAttribute("message", "Password reset OTP sent to your email");
            redirectAttributes.addAttribute("email", email);
            return "redirect:/forgot-password";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("email", email);
            return "redirect:/forgot-password";
        }
    }

    @PostMapping("/forgot-password/reset")
    public String resetPasswordWithOtp(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes
    ) {
        try {
            authService.resetPasswordWithOtp(email, otp, newPassword);
            redirectAttributes.addAttribute("message", "Password reset successful. Please login.");
            return "redirect:/login";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("email", email);
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return "redirect:/admin/home";
        }

        boolean isMentor = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MENTOR".equals(a.getAuthority()));
        if (isMentor) {
            return "redirect:/mentor/home";
        }

        return "redirect:/participant/home";
    }

    @GetMapping("/admin/home")
    public String adminHome(Model model) {
        List<UserDTO> users = adminService.getAllUsers();
        LocalDateTime now = LocalDateTime.now();
        long participants = users.stream().filter(u -> u.getRole() == Role.PARTICIPANT).count();
        long mentors = users.stream().filter(u -> u.getRole() == Role.MENTOR).count();
        long activeExams = examService.getActiveExams().size();
        long finishedExams = examRepository.findAll().stream()
                .filter(exam -> exam.getEndTime() != null && exam.getEndTime().isBefore(now))
                .count();

        model.addAttribute("participantCount", participants);
        model.addAttribute("mentorCount", mentors);
        model.addAttribute("activeExamCount", activeExams);
        model.addAttribute("finishedExamCount", finishedExams);
        return "admin-dashboard";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model) {
        model.addAttribute("users", adminService.getAllUsers().stream()
                .filter(user -> user.getRole() != Role.ADMIN)
                .toList());
        return "admin-users";
    }

    @GetMapping("/admin/exams")
    public String adminExams(Model model) {
        LocalDateTime now = LocalDateTime.now();
        List<Exam> exams = examRepository.findAll().stream()
                .sorted(Comparator.comparing(Exam::getStartTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Exam exam : exams) {
            Map<String, Object> row = new HashMap<>();
            row.put("examId", exam.getId());
            row.put("title", exam.getTitle());
            row.put("startTime", exam.getStartTime());
            row.put("endTime", exam.getEndTime());
            row.put("active", exam.isActive());
            row.put("questionCount", questionRepository.findByExamId(exam.getId()).size());
            row.put("registeredCount", resultRepository.findByExamId(exam.getId()).size());
            row.put("submittedCount", resultRepository.findByExamIdAndSubmittedTrue(exam.getId()).size());
            row.put("finished", exam.getEndTime() != null && now.isAfter(exam.getEndTime()));
            rows.add(row);
        }
        model.addAttribute("exams", rows);
        return "admin-exams";
    }

    @GetMapping("/admin/exams/{examId}/results")
    public String adminExamResults(@PathVariable String examId, Model model) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        List<Result> allResults = resultRepository.findByExamId(examId);
        List<Result> submittedResults = resultRepository.findByExamIdAndSubmittedTrue(examId);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Result result : submittedResults) {
            Map<String, Object> row = new HashMap<>();
            User student = userRepository.findById(result.getStudentId()).orElse(null);
            row.put("studentName", student != null ? student.getName() : "Unknown");
            row.put("studentEmail", student != null ? student.getEmail() : "Unknown");
            row.put("score", result.getScore());
            row.put("submittedAt", result.getSubmittedAt());
            row.put("attemptStartAt", result.getAttemptStartAt());
            row.put("attemptEndAt", result.getAttemptEndAt());
            rows.add(row);
        }

        model.addAttribute("exam", exam);
        model.addAttribute("registeredCount", allResults.size());
        model.addAttribute("submittedCount", submittedResults.size());
        model.addAttribute("resultRows", rows);
        return "admin-exam-results";
    }

    @PostMapping("/admin/approve-mentor/{mentorId}")
    public String approveMentor(@PathVariable String mentorId, RedirectAttributes redirectAttributes) {
        adminService.approveMentor(mentorId);
        redirectAttributes.addFlashAttribute("message", "Mentor approved successfully");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/block-user/{userId}")
    public String blockUser(@PathVariable String userId, RedirectAttributes redirectAttributes) {
        adminService.blockUser(userId);
        redirectAttributes.addFlashAttribute("message", "User blocked successfully");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/unblock-user/{userId}")
    public String unblockUser(@PathVariable String userId, RedirectAttributes redirectAttributes) {
        adminService.unblockUser(userId);
        redirectAttributes.addFlashAttribute("message", "User unblocked successfully");
        return "redirect:/admin/users";
    }

    @GetMapping("/mentor/home")
    public String mentorHome(Authentication authentication, Model model) {
        List<ExamDTO> mentorExams = examService.getExamsByMentor(authentication.getName());
        LocalDateTime now = LocalDateTime.now();
        long activeExamCount = mentorExams.stream()
                .filter(exam -> exam.getStartTime() != null && exam.getEndTime() != null)
                .filter(exam -> !now.isBefore(exam.getStartTime()) && now.isBefore(exam.getEndTime()))
                .count();

        int questionCount = 0;
        for (ExamDTO exam : mentorExams) {
            questionCount += questionRepository.findByExamId(exam.getExamId()).size();
        }

        model.addAttribute("activeExamCount", activeExamCount);
        model.addAttribute("questionCount", questionCount);
        model.addAttribute("totalExamCount", mentorExams.size());
        return "mentor-dashboard";
    }

    @GetMapping("/mentor/create-exam")
    public String mentorCreateExamPage() {
        return "mentor-create-exam";
    }

    @GetMapping("/mentor/exams")
    public String mentorManageExams(Authentication authentication, Model model) {
        List<ExamDTO> mentorExams = examService.getExamsByMentor(authentication.getName());
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamDTO exam : mentorExams) {
            Map<String, Object> row = new HashMap<>();
            row.put("exam", exam);
            int questionCount = questionRepository.findByExamId(exam.getExamId()).size();
            row.put("questionCount", questionCount);
            row.put("canRegister", questionCount >= 3);
            row.put("editable", exam.getStartTime() == null || now.isBefore(exam.getStartTime()));
            rows.add(row);
        }
        model.addAttribute("mentorExamRows", rows);
        model.addAttribute("examCount", mentorExams.size());
        return "mentor-manage-exams";
    }

    @GetMapping("/mentor/results")
    public String mentorResults(Authentication authentication, Model model) {
        List<ExamDTO> mentorExams = examService.getExamsByMentor(authentication.getName());
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamDTO exam : mentorExams) {
            if (exam.getEndTime() == null || now.isBefore(exam.getEndTime())) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("exam", exam);
            row.put("registeredCount", resultRepository.findByExamId(exam.getExamId()).size());
            row.put("submittedCount", resultRepository.findByExamIdAndSubmittedTrue(exam.getExamId()).size());
            rows.add(row);
        }
        model.addAttribute("mentorResultRows", rows);
        return "mentor-results";
    }

    @PostMapping("/mentor/exams")
    public String createExam(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam int durationMinutes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        ExamDTO examDTO = new ExamDTO();
        examDTO.setTitle(title);
        examDTO.setDescription(description);
        examDTO.setDurationMinutes(durationMinutes);
        examDTO.setStartTime(startTime);
        examDTO.setEndTime(endTime);

        examService.createExam(examDTO, authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Exam created successfully");
        return "redirect:/mentor/exams";
    }

    @PostMapping("/mentor/questions")
    public String addQuestion(
            @RequestParam String examId,
            @RequestParam String questionText,
            @RequestParam String options,
            @RequestParam String correctAnswer,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        QuestionDTO questionDTO = new QuestionDTO();
        questionDTO.setExamId(examId);
        questionDTO.setQuestionText(questionText);
        questionDTO.setOptions(Arrays.stream(options.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList()));
        questionDTO.setCorrectAnswer(correctAnswer);

        questionService.addQuestion(questionDTO, authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Question added successfully");
        return "redirect:/mentor/exams/" + examId;
    }

    @GetMapping("/mentor/exams/{examId}")
    public String mentorExamDetail(
            @PathVariable String examId,
            Authentication authentication,
            Model model
    ) {
        ExamDTO exam = examService.getMentorExamById(examId, authentication.getName());
        List<QuestionDTO> questions = questionService.getQuestionsByExam(examId);
        LocalDateTime now = LocalDateTime.now();
        boolean examFinished = exam.getEndTime() != null && now.isAfter(exam.getEndTime());
        boolean editLocked = exam.getStartTime() != null && !now.isBefore(exam.getStartTime());

        model.addAttribute("exam", exam);
        model.addAttribute("questions", questions);
        model.addAttribute("questionCount", questions.size());
        model.addAttribute("registeredCount", resultRepository.findByExamId(examId).size());
        model.addAttribute("submittedCount", resultRepository.findByExamIdAndSubmittedTrue(examId).size());
        model.addAttribute("examFinished", examFinished);
        model.addAttribute("editLocked", editLocked);

        if (examFinished) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (var result : resultRepository.findByExamIdAndSubmittedTrue(examId)) {
                Map<String, Object> row = new HashMap<>();
                row.put("score", result.getScore());
                row.put("submittedAt", result.getSubmittedAt());
                row.put("attemptStartAt", result.getAttemptStartAt());
                row.put("attemptEndAt", result.getAttemptEndAt());
                User student = userRepository.findById(result.getStudentId()).orElse(null);
                row.put("studentName", student != null ? student.getName() : "Unknown");
                row.put("studentEmail", student != null ? student.getEmail() : "Unknown");
                rows.add(row);
            }
            model.addAttribute("resultRows", rows);
        }

        return "mentor-exam-detail";
    }

    @GetMapping("/mentor/questions/template")
    @ResponseBody
    public ResponseEntity<byte[]> mentorQuestionTemplate() throws Exception {
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

    @PostMapping("/mentor/questions/upload")
    public String uploadQuestionsExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String examId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        int inserted = examId != null && !examId.isBlank()
                ? questionService.addQuestionsFromExcel(file, authentication.getName(), examId)
                : questionService.addQuestionsFromExcel(file, authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Questions uploaded: " + inserted);
        if (examId != null && !examId.isBlank()) {
            return "redirect:/mentor/exams/" + examId;
        }
        return "redirect:/mentor/exams";
    }

    @GetMapping({
            "/participant/home",
            "/participant/dashboard",
            "/participate/home",
            "/participate/dashboard"
    })
    public String participantHome(Authentication authentication, Model model) {
        User student = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        LocalDateTime now = LocalDateTime.now();
        List<ExamDTO> activeExams = examService.getActiveExams();
        List<Result> myResults = resultRepository.findByStudentId(student.getId());
        Set<String> registeredExamIds = myResults.stream().map(Result::getExamId).collect(Collectors.toCollection(HashSet::new));
        Set<String> submittedExamIds = myResults.stream().filter(Result::isSubmitted).map(Result::getExamId).collect(Collectors.toCollection(HashSet::new));

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> startNowRows = new ArrayList<>();
        for (ExamDTO exam : activeExams) {
            Map<String, Object> row = new HashMap<>();
            boolean registered = registeredExamIds.contains(exam.getExamId());
            boolean submitted = submittedExamIds.contains(exam.getExamId());
            boolean inWindow = exam.getStartTime() != null
                    && exam.getEndTime() != null
                    && !now.isBefore(exam.getStartTime())
                    && now.isBefore(exam.getEndTime());

            row.put("exam", exam);
            row.put("registered", registered);
            row.put("submitted", submitted);
            row.put("canStart", registered && !submitted && inWindow);
            rows.add(row);
            if (registered && !submitted && inWindow) {
                startNowRows.add(row);
            }
        }

        model.addAttribute("participantExamRows", rows);
        model.addAttribute("startNowRows", startNowRows);
        model.addAttribute("activeExamCount", activeExams.size());
        return "participant-dashboard";
    }

    @GetMapping({"/participant/exams", "/participate/exams"})
    public String participantExams(Authentication authentication, Model model) {
        User student = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        List<ExamDTO> activeExams = examService.getActiveExams();
        Set<String> registeredExamIds = resultRepository.findByStudentId(student.getId())
                .stream()
                .map(Result::getExamId)
                .collect(Collectors.toCollection(HashSet::new));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamDTO exam : activeExams) {
            Map<String, Object> row = new HashMap<>();
            row.put("exam", exam);
            row.put("registered", registeredExamIds.contains(exam.getExamId()));
            row.put("questionCount", questionRepository.findByExamId(exam.getExamId()).size());
            row.put("registeredCount", resultRepository.findByExamId(exam.getExamId()).size());
            rows.add(row);
        }
        model.addAttribute("participantExamRows", rows);
        return "participant-exams";
    }

    @GetMapping({"/participant/exams/{examId}", "/participate/exams/{examId}"})
    public String participantExamDetail(
            @PathVariable String examId,
            Authentication authentication,
            Model model
    ) {
        User student = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        LocalDateTime now = LocalDateTime.now();
        Result result = resultRepository.findByExamIdAndStudentId(examId, student.getId()).orElse(null);
        boolean registered = result != null;
        boolean submitted = result != null && result.isSubmitted();
        boolean canStart = registered
                && !submitted
                && exam.getStartTime() != null
                && exam.getEndTime() != null
                && !now.isBefore(exam.getStartTime())
                && now.isBefore(exam.getEndTime());

        model.addAttribute("exam", exam);
        model.addAttribute("questionCount", questionRepository.findByExamId(examId).size());
        model.addAttribute("registeredCount", resultRepository.findByExamId(examId).size());
        model.addAttribute("registered", registered);
        model.addAttribute("submitted", submitted);
        model.addAttribute("canStart", canStart);
        return "participant-exam-detail";
    }

    @GetMapping({"/participant/exam-session/{examId}", "/participate/exam-session/{examId}"})
    public String participantExamSession(
            @PathVariable String examId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User student = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
            Result registration = resultRepository.findByExamIdAndStudentId(examId, student.getId())
                    .orElseThrow(() -> new BadRequestException("Please register before starting exam"));
            if (registration.isSubmitted()) {
                throw new BadRequestException("Exam already submitted");
            }

            long remainingSeconds = studentService.getRemainingSeconds(examId, authentication.getName());
            if (remainingSeconds <= 0) {
                throw new BadRequestException("Exam duration is over");
            }

            model.addAttribute("exam", exam);
            model.addAttribute("selectedExamId", examId);
            model.addAttribute("questions", questionService.getQuestionsByExam(examId));
            model.addAttribute("remainingSeconds", remainingSeconds);
            return "participant-exam-session";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            return "redirect:/participant/home";
        }
    }

    @GetMapping({"/participant/results", "/participate/results"})
    public String participantResults(Authentication authentication, Model model) {
        List<ResultDTO> results = studentService.getMyResults(authentication.getName());
        model.addAttribute("results", results);
        return "participant-results";
    }

    @PostMapping({"/participant/register-exam/{examId}", "/participate/register-exam/{examId}"})
    public String registerExam(
            @PathVariable String examId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        studentService.registerForExam(examId, authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Registered for exam successfully");
        return "redirect:/participant/exams/" + examId;
    }

    @PostMapping({"/participant/submit-exam", "/participate/submit-exam"})
    public String submitExam(
            @RequestParam String examId,
            @RequestParam Map<String, String> formData,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        Map<String, String> answers = new HashMap<>();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (entry.getKey().startsWith("answer_")) {
                String questionId = entry.getKey().substring("answer_".length());
                answers.put(questionId, entry.getValue());
            }
        }

        ResultDTO resultDTO = new ResultDTO();
        resultDTO.setExamId(examId);
        resultDTO.setAnswers(answers);

        var result = studentService.submitExam(resultDTO, authentication.getName());
        redirectAttributes.addFlashAttribute("message",
                "Exam submitted. Score: " + result.getScore());
        return "redirect:/participant/results";
    }
}
