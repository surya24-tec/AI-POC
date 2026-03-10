package com.ai.interview.service;

import com.ai.interview.model.*;
import com.ai.interview.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InterviewService {

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

    public ConversationSession startSession(String username) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setEmail(username + "@example.com");
                    return userRepository.save(newUser);
                });

        ConversationSession session = new ConversationSession();
        session.setUser(user);
        session.setStartTime(LocalDateTime.now());
        session.setStatus(ConversationSession.SessionStatus.ACTIVE);

        return sessionRepository.save(session);
    }

    public Question getNextQuestion(Long sessionId) {
        Optional<ConversationSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            // Return a fallback question if session is missing (instead of null/crashing)
            Question fallback = new Question();
            fallback.setContent("Session not found. Please refresh and start again.");
            fallback.setCategory("System");
            return fallback;
        }

        ConversationSession session = sessionOpt.get();
        List<Answer> previousAnswers = answerRepository.findBySession(session);

        StringBuilder historyBuilder = new StringBuilder();
        for (Answer ans : previousAnswers) {
            historyBuilder.append("AI: ").append(ans.getQuestion().getContent()).append("\n");
            historyBuilder.append("Human: ").append(ans.getContent()).append("\n");
        }

        String aiQuestionText = geminiService.generateQuestion(historyBuilder.toString());

        Question question = new Question();
        question.setContent(aiQuestionText);
        question.setCategory("Dynamic AI");
        question.setAiGenerated(true);
        return questionRepository.save(question);
    }

    public Answer submitAnswer(Long sessionId, Long questionId, String content) {
        ConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Answer answer = new Answer();
        answer.setSession(session);
        answer.setQuestion(question);
        answer.setContent(content);
        answer.setTimestamp(LocalDateTime.now());
        answer.setSubScore(70); // Default score

        return answerRepository.save(answer);
    }

    public String getFinalFeedback(Long sessionId) {
        Optional<ConversationSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty())
            return "Session expired.";

        ConversationSession session = sessionOpt.get();
        List<Answer> answers = answerRepository.findBySession(session);

        StringBuilder transcript = new StringBuilder();
        for (Answer ans : answers) {
            transcript.append("Q: ").append(ans.getQuestion().getContent()).append("\n");
            transcript.append("A: ").append(ans.getContent()).append("\n\n");
        }

        String prompt = "Evaluate this Software Development interview transcript. " +
                "Provide a final score out of 100 and a short summary.\n\n" + transcript.toString();

        String feedback = geminiService.generateQuestion(prompt);

        session.setScore(85);
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
