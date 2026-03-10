package com.ai.interview.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.*;
import java.util.*;

@Service
public class GeminiService {

    @Value("${ai.provider.api.key}")
    private String apiKey;

    @Value("${ai.provider.api.base.url}")
    private String baseUrl;

    @Value("${ai.provider.model}")
    private String model;

    private final RestTemplate restTemplate;

    public GeminiService() {
        // Set request timeouts to avoid hanging the application
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds
        factory.setReadTimeout(15000); // 15 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    private final List<String> fallbackQuestions = Arrays.asList(
            "Tell me about a time you had to work with a difficult team member. How did you handle it?",
            "How do you prioritize your tasks when you have multiple deadlines to meet?",
            "Describe a situation where you had to learn a new skill quickly. What was your approach?",
            "How do you stay updated with the latest trends and technologies in your field?",
            "What are your greatest strengths and how do they help you in a professional environment?",
            "Tell me about a project that didn't go as planned. What did you learn from it?",
            "How do you handle high-pressure situations or tight deadlines?",
            "Why are you interested in this position, and what unique value can you bring to the team?");

    private final Random random = new Random();

    public String generateQuestion(String history) {
        // Fix for malformed URL caused by incorrect application.properties
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                + apiKey;

        String systemPrompt = "You are a professional AI Interviewer. " +
                "Task: Ask one communication/technical question. " +
                "Rule: Return ONLY the question text. Do not ask multiple questions. " +
                "No meta-commentary. Use the history to avoid repeating questions.";

        // Format for Google Gemini API
        Map<String, Object> textPart = Map.of("text", systemPrompt + "\n\nHistory:\n" + history + "\n\nNext Question:");
        Map<String, Object> contentPart = Map.of("parts", List.of(textPart));
        Map<String, Object> requestBody = Map.of("contents", List.of(contentPart));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("Processing AI Question Request...");
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");

                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");

                    if (parts != null && !parts.isEmpty()) {
                        String result = ((String) parts.get(0).get("text")).trim();
                        if (!result.isEmpty())
                            return result;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("AI Provider Error: " + e.getMessage());
            // Fail gracefully by picking a random fallback question
            String fallback = fallbackQuestions.get(random.nextInt(fallbackQuestions.size()));
            System.out.println("Using fallback question: " + fallback);
            return fallback;
        }

        return fallbackQuestions.get(random.nextInt(fallbackQuestions.size()));
    }

    public String generateContent(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                + apiKey;

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contentPart = Map.of("parts", List.of(textPart));
        Map<String, Object> requestBody = Map.of("contents", List.of(contentPart));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");

                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
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
            }
            return "No content generated from Gemini.";
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            System.err.println("Gemini HTTP Error: " + e.getStatusCode() + " - " + errorBody);
            // Replace the generic fallback message with the actual Google Gemini Error so
            // we can see it in the UI!
            return "API Error: Unable to evaluate answer. The Google Gemini API returned an error: " + e.getStatusCode()
                    + ". Check backend logs for more details.";
        } catch (Exception e) {
            System.err.println("Gemini Server Error in generateContent: " + e.getMessage());
            return "API Error: Unable to connect to Google Gemini API. " + e.getMessage();
        }
    }
}
