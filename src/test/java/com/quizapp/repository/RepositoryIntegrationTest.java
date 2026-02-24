package com.quizapp.repository;

import com.quizapp.entity.Exam;
import com.quizapp.entity.Question;
import com.quizapp.entity.Result;
import com.quizapp.entity.Role;
import com.quizapp.entity.User;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExamRepository examRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private ResultRepository resultRepository;
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;
    @Value("${spring.data.mongodb.database}")
    private String mongoDatabase;
    private boolean mongoAvailable;

    @BeforeEach
    void requireRunningMongo() {
        mongoAvailable = isMongoAvailable();
        Assumptions.assumeTrue(mongoAvailable, "Skipping integration tests because MongoDB is not reachable on localhost:27017");
    }

    @AfterEach
    void cleanUp() {
        if (!mongoAvailable) {
            return;
        }
        try {
            resultRepository.deleteAll();
            questionRepository.deleteAll();
            examRepository.deleteAll();
            userRepository.deleteAll();
        } catch (Exception ignored) {
            // Ignore cleanup failures when DB is unavailable; tests are skipped in that case.
        }
    }

    @Test
    void userRepository_shouldFindByEmailAndRoleApproval() {
        User user = User.builder()
                .name("Mentor One")
                .email("mentor.repo@test.com")
                .password("x")
                .role(Role.MENTOR)
                .approved(true)
                .build();
        userRepository.save(user);

        assertTrue(userRepository.findByEmail("mentor.repo@test.com").isPresent());
        assertTrue(userRepository.existsByEmail("mentor.repo@test.com"));

        List<User> approvedMentors = userRepository.findByRoleAndApproved(Role.MENTOR, true);
        assertFalse(approvedMentors.isEmpty());
    }

    @Test
    void examRepository_shouldFindActiveExamsByTimeWindow() {
        LocalDateTime now = LocalDateTime.now();
        Exam exam = Exam.builder()
                .title("Repo Exam")
                .description("desc")
                .durationMinutes(30)
                .startTime(now.minusMinutes(10))
                .endTime(now.plusMinutes(20))
                .active(true)
                .mentorId("m1")
                .build();
        examRepository.save(exam);

        List<Exam> active = examRepository.findByStartTimeBeforeAndEndTimeAfter(now, now);
        assertFalse(active.isEmpty());
        assertEquals("Repo Exam", active.get(0).getTitle());

        List<Exam> mentorExams = examRepository.findByMentorIdOrderByStartTimeDesc("m1");
        assertFalse(mentorExams.isEmpty());

        List<Exam> visible = examRepository.findByActiveTrueAndEndTimeAfterOrderByStartTimeAsc(now.minusMinutes(1));
        assertFalse(visible.isEmpty());
    }

    @Test
    void questionRepository_shouldFindByExamId() {
        Question question = Question.builder()
                .examId("exam-repo-1")
                .questionText("2+2?")
                .options(List.of("3", "4"))
                .correctAnswer("4")
                .build();
        questionRepository.save(question);

        List<Question> questions = questionRepository.findByExamId("exam-repo-1");
        assertEquals(1, questions.size());
    }

    @Test
    void resultRepository_shouldSupportExamStudentQueries() {
        Result result = Result.builder()
                .examId("exam-r1")
                .studentId("student-r1")
                .score(2)
                .submitted(true)
                .answers(Map.of("q1", "A"))
                .submittedAt(LocalDateTime.now())
                .build();
        resultRepository.save(result);

        assertTrue(resultRepository.existsByExamIdAndStudentId("exam-r1", "student-r1"));
        assertTrue(resultRepository.findByExamIdAndStudentId("exam-r1", "student-r1").isPresent());
        assertEquals(1, resultRepository.findByStudentId("student-r1").size());
        assertEquals(1, resultRepository.findByExamId("exam-r1").size());
        assertEquals(1, resultRepository.findByExamIdAndSubmittedTrue("exam-r1").size());
    }

    private boolean isMongoAvailable() {
        try (MongoClient client = MongoClients.create(mongoUri)) {
            String databaseName = mongoDatabase;
            if (databaseName == null || databaseName.isBlank()) {
                ConnectionString cs = new ConnectionString(mongoUri);
                databaseName = cs.getDatabase();
            }
            if (databaseName == null || databaseName.isBlank()) {
                return false;
            }
            client.getDatabase(databaseName).runCommand(new org.bson.Document("ping", 1));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
