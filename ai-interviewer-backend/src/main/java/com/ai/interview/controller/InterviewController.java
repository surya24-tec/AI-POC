package com.ai.interview.controller;

import com.ai.interview.model.*;
import com.ai.interview.service.InterviewService;
import com.ai.interview.service.GeminiService;
import com.ai.interview.service.InterviewPromptService;
import com.ai.interview.dto.AiEvaluationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@CrossOrigin(origins = "*")
public class InterviewController {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private InterviewPromptService promptService;

    // ── Per-session rate limiter: max 5 requests per minute per session ──────
    private final ConcurrentHashMap<Long, long[]> sessionRateMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 5;

    private boolean isRateLimited(Long sessionId) {
        long now = System.currentTimeMillis();
        sessionRateMap.putIfAbsent(sessionId, new long[]{0, now});
        long[] data = sessionRateMap.get(sessionId);

        // Reset window every 60 seconds
        if (now - data[1] > 60_000) {
            data[0] = 0;
            data[1] = now;
        }

        if (data[0] >= MAX_REQUESTS_PER_MINUTE) {
            return true; // Rate limited
        }
        data[0]++;
        return false;
    }

    // ── Endpoints ───────────────────────────────────────────────────────────

    @PostMapping("/api/interview/start")
    public ResponseEntity<ConversationSession> startInterview(@RequestParam String username) {
        System.out.println(">>> Backend: Starting session for user: " + username);
        return ResponseEntity.ok(interviewService.startSession(username));
    }

    @GetMapping("/api/interview/next-question/{sessionId}")
    public ResponseEntity<Question> getNextQuestion(@PathVariable Long sessionId) {
        System.out.println(">>> Backend: Fetching question for session ID: " + sessionId);

        if (isRateLimited(sessionId)) {
            System.out.println(">>> Backend: Rate limited session " + sessionId);
            Question rateMsg = new Question();
            rateMsg.setContent("Please wait a moment before requesting the next question.");
            rateMsg.setCategory("System");
            return ResponseEntity.status(429).body(rateMsg);
        }

        Question q = interviewService.getNextQuestion(sessionId);

        if (q == null || q.getContent() == null) {
            System.out.println(">>> Backend: Warning - No question found.");
            return ResponseEntity.noContent().build();
        }

        System.out.println(">>> Backend: DISPATCHING QUESTION -> " + q.getContent());
        return ResponseEntity.ok(q);
    }

    @PostMapping("/api/interview/submit-answer")
    public ResponseEntity<Answer> submitAnswer(@RequestBody Map<String, Object> payload) {
        Long sessionId = Long.valueOf(payload.get("sessionId").toString());
        Long questionId = Long.valueOf(payload.get("questionId").toString());
        String content = payload.get("content").toString();

        if (isRateLimited(sessionId)) {
            System.out.println(">>> Backend: Rate limited session " + sessionId);
            return ResponseEntity.status(429).build();
        }

        System.out.println(">>> Backend: Answer received for session " + sessionId);
        return ResponseEntity.ok(interviewService.submitAnswer(sessionId, questionId, content));
    }

    @GetMapping("/api/interview/history/{sessionId}")
    public List<Answer> getHistory(@PathVariable Long sessionId) {
        return interviewService.getConversationHistory(sessionId);
    }

    @PostMapping("/api/interview/finish/{sessionId}")
    public ResponseEntity<String> finishInterview(@PathVariable Long sessionId) {
        return ResponseEntity.ok(interviewService.getFinalFeedback(sessionId));
    }

    // AI evaluation endpoint
    @PostMapping("/ai/evaluate")
    public ResponseEntity<String> evaluateAnswer(@RequestBody AiEvaluationRequest request) {
        try {
            String prompt = promptService.buildPrompt(request.getQuestion(), request.getAnswer());
            String response = geminiService.generateContent(prompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error evaluating answer: " + e.getMessage());
            return ResponseEntity.status(500).body("Fallback: Error while talking to Gemini API.");
        }
    }
}
