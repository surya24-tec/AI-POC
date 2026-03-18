package com.ai.interview.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    private final RestTemplate restTemplate;

    // --- Rate limiter: max 15 requests per minute to avoid 429 ---
    private static final int MAX_REQUESTS_PER_MINUTE = 15;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

    public GeminiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000); // 30 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Simple rate limiter — blocks if quota within the current minute is exceeded.
     */
    private void waitIfRateLimited() {
        long now = System.currentTimeMillis();
        long windowMs = now - windowStart.get();

        // Reset window every 60 seconds
        if (windowMs > 60_000) {
            requestCount.set(0);
            windowStart.set(now);
        }

        if (requestCount.get() >= MAX_REQUESTS_PER_MINUTE) {
            long waitMs = 60_000 - windowMs;
            if (waitMs > 0) {
                System.out.println("Rate limiter: Waiting " + (waitMs / 1000) + "s to avoid 429...");
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            requestCount.set(0);
            windowStart.set(System.currentTimeMillis());
        }
        requestCount.incrementAndGet();
    }

    // ── Fallback questions when API is unavailable ──────────────────────────

    private final List<String> fallbackQuestions = Arrays.asList(
            "Tell me about yourself and your professional background.",
            "Explain a challenging project you worked on recently. What was your role?",
            "What are your strengths and weaknesses as a software developer?",
            "Why should we hire you for this position?",
            "How do you handle disagreements with team members during a project?",
            "Describe a time when you had to learn a new technology quickly. How did you approach it?",
            "What is your approach to debugging a complex production issue?",
            "Where do you see yourself in the next 3-5 years?",
            "How do you prioritize tasks when working on multiple projects simultaneously?",
            "Tell me about a time when you failed. What did you learn from it?");

    private final Random random = new Random();

    // ── Generate a question (with retry + fallback) ─────────────────────────

    public String generateQuestion(String history) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey.trim();

        String systemPrompt = "You are a professional technical interviewer.\n\n" +
                "Ask one clear, concise interview question based on the job role: Software Developer.\n\n" +
                "Rules:\n" +
                "- Ask only ONE question\n" +
                "- Keep it short and realistic\n" +
                "- Do not add explanation\n" +
                "- Do not repeat previous questions\n\n" +
                "Output format:\n" +
                "Question: <your question>";

        Map<String, Object> textPart = Map.of("text",
                systemPrompt + "\n\nHistory:\n" + history + "\n\nNext Question:");
        Map<String, Object> contentPart = Map.of("parts", List.of(textPart));
        Map<String, Object> requestBody = Map.of("contents", List.of(contentPart));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                waitIfRateLimited();
                System.out.println("Processing AI Question Request (attempt " + attempt + "/" + maxRetries + ")...");
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    String text = extractText(response.getBody());
                    if (text != null)
                        return text;
                }
            } catch (org.springframework.web.client.HttpStatusCodeException e) {
                System.err.println("Gemini HTTP Error (attempt " + attempt + "/" + maxRetries + "): "
                        + e.getStatusCode());
                if (e.getStatusCode().value() == 429 && attempt < maxRetries) {
                    System.out.println("Rate limited by API. Waiting 10 seconds before retry...");
                    try {
                        Thread.sleep(10_000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
            } catch (Exception e) {
                System.err.println("AI Provider Error: " + e.getMessage());
            }
            break;
        }

        // Fail gracefully — use fallback question
        String fallback = fallbackQuestions.get(random.nextInt(fallbackQuestions.size()));
        System.out.println("Using fallback question: " + fallback);
        return fallback;
    }

    // ── Generate content / evaluate answer (with retry) ─────────────────────

    public String generateContent(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey.trim();

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contentPart = Map.of("parts", List.of(textPart));
        Map<String, Object> requestBody = Map.of("contents", List.of(contentPart));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                waitIfRateLimited();
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    String text = extractText(response.getBody());
                    if (text != null)
                        return text;
                }
                return "No content generated from Gemini.";
            } catch (org.springframework.web.client.HttpStatusCodeException e) {
                System.err.println("Gemini HTTP Error (attempt " + attempt + "/" + maxRetries + "): "
                        + e.getStatusCode());
                if (e.getStatusCode().value() == 429 && attempt < maxRetries) {
                    System.out.println("Rate limited by API. Waiting 10 seconds before retry...");
                    try {
                        Thread.sleep(10_000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                return "API Error: Gemini returned " + e.getStatusCode() + ". Please try again shortly.";
            } catch (Exception e) {
                System.err.println("Gemini Error: " + e.getMessage());
                return "API Error: Unable to connect to Gemini. " + e.getMessage();
            }
        }
        return "API Error: All retry attempts exhausted. Please wait a minute and try again.";
    }

    // ── Shared helper to extract text from Gemini response ──────────────────

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> body) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> contentMap = (Map<String, Object>) candidates.get(0).get("content");
                if (contentMap != null && contentMap.containsKey("parts")) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        String result = (String) parts.get(0).get("text");
                        if (result != null && !result.trim().isEmpty()) {
                            return result.trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
        }
        return null;
    }
}
