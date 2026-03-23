package com.ai.interview.service;

import com.ai.interview.model.*;
import com.ai.interview.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class InterviewService {

    // Hybrid model: first N questions from DB, then AI
    private static final int DB_QUESTION_LIMIT = 3;

    @Autowired
    private ConversationSessionRepository sessionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private EmailService emailService;

    public ConversationSession startSession(String username) {
        java.util.concurrent.atomic.AtomicBoolean isNewUser = new java.util.concurrent.atomic.AtomicBoolean(false);
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    isNewUser.set(true);
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setEmail(username.contains("@") ? username : username + "@example.com");
                    return userRepository.save(newUser);
                });

        ConversationSession session = new ConversationSession();
        session.setUser(user);
        session.setStartTime(LocalDateTime.now());
        session.setStatus(ConversationSession.SessionStatus.ACTIVE);

        ConversationSession savedSession = sessionRepository.save(session);

        // Send Welcome email if the user used an email address
        if (username.contains("@")) {
            emailService.sendWelcomeEmail(username);
        }

        return savedSession;
    }

    // ── Hybrid Question Model ───────────────────────────────────────────────
    // Step 1-3: Questions from DB (no API call — saves quota)
    // Step 4+:  Questions from AI (Gemini)
    // Retry:    Fallback questions (handled inside GeminiService)
    public Question getNextQuestion(Long sessionId) {
        Optional<ConversationSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            Question fallback = new Question();
            fallback.setContent("Session not found. Please refresh and start again.");
            fallback.setCategory("System");
            return fallback;
        }

        ConversationSession session = sessionOpt.get();
        List<Answer> previousAnswers = answerRepository.findBySession(session);
        int questionNumber = previousAnswers.size(); // 0-indexed: how many answered so far

        // ── STEP 1: First N questions from DB (no AI call) ──────────────────
        if (questionNumber < DB_QUESTION_LIMIT) {
            Question dbQuestion = getUnaskedDbQuestion(previousAnswers);
            if (dbQuestion != null) {
                System.out.println(">>> Hybrid: Using DB question #" + (questionNumber + 1)
                        + " -> " + dbQuestion.getContent());
                return dbQuestion;
            }
        }

        // ── STEP 2: Remaining questions from AI ────────────────────────────
        StringBuilder historyBuilder = new StringBuilder();
        for (Answer ans : previousAnswers) {
            historyBuilder.append("AI: ").append(ans.getQuestion().getContent()).append("\n");
            historyBuilder.append("Human: ").append(ans.getContent()).append("\n");
        }

        String aiQuestionText = geminiService.generateQuestion(historyBuilder.toString());
        System.out.println(">>> Hybrid: Using AI question #" + (questionNumber + 1)
                + " -> " + aiQuestionText);

        Question question = new Question();
        question.setContent(aiQuestionText);
        question.setCategory("Dynamic AI");
        question.setAiGenerated(true);
        return questionRepository.save(question);
    }

    /**
     * Find a DB-seeded question that hasn't been asked yet in this session.
     */
    private Question getUnaskedDbQuestion(List<Answer> previousAnswers) {
        // Get IDs of questions already asked
        Set<Long> askedIds = previousAnswers.stream()
                .map(a -> a.getQuestion().getId())
                .collect(Collectors.toSet());

        // Get pre-seeded DB questions (not AI generated)
        List<Question> dbQuestions = questionRepository.findByIsAiGenerated(false);

        // Return first unasked DB question
        for (Question q : dbQuestions) {
            if (!askedIds.contains(q.getId())) {
                return q;
            }
        }
        return null; // All DB questions used — will fall through to AI
    }

    // ── Submit Answer (handles missing questionId) ──────────────────────────
    public Answer submitAnswer(Long sessionId, Long questionId, String content) {
        ConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Question question;
        if (questionId == null || questionId <= 0) {
            question = new Question();
            question.setContent("Communication Question");
            question.setCategory("Communication");
            question.setAiGenerated(false);
            question = questionRepository.save(question);
        } else {
            question = questionRepository.findById(questionId).orElseGet(() -> {
                Question fallback = new Question();
                fallback.setContent("Question (ref: " + questionId + ")");
                fallback.setCategory("Dynamic AI");
                fallback.setAiGenerated(true);
                return questionRepository.save(fallback);
            });
        }

        Answer answer = new Answer();
        answer.setSession(session);
        answer.setQuestion(question);
        answer.setContent(content);
        answer.setTimestamp(LocalDateTime.now());
        answer.setSubScore(70);

        return answerRepository.save(answer);
    }

    // ── Final Feedback with Dynamic Score ────────────────────────────────────
    public String getFinalFeedback(Long sessionId) {
        Optional<ConversationSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty())
            return "Session expired.";

        ConversationSession session = sessionOpt.get();
        List<Answer> answers = answerRepository.findBySession(session);

        StringBuilder transcript = new StringBuilder();
        int totalSubScore = 0;
        for (Answer ans : answers) {
            transcript.append("Q: ").append(ans.getQuestion().getContent()).append("\n");
            transcript.append("A: ").append(ans.getContent()).append("\n\n");
            totalSubScore += ans.getSubScore();
        }

        int dynamicScore = answers.isEmpty() ? 0 : totalSubScore / answers.size();

        String prompt = "Evaluate this Software Development interview transcript. " +
                "Provide a final score out of 100 and a short summary.\n\n" + transcript.toString();

        String feedback = geminiService.generateContent(prompt);

        session.setScore(dynamicScore);
        session.setFeedback(feedback);
        session.setEndTime(LocalDateTime.now());
        session.setStatus(ConversationSession.SessionStatus.COMPLETED);

        sessionRepository.save(session);
        return feedback;
    }

    public List<Answer> getConversationHistory(Long sessionId) {
        Optional<ConversationSession> session = sessionRepository.findById(sessionId);
        if (session.isEmpty())
            return List.of();
        return answerRepository.findBySession(session.get());
    }
}
