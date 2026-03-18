package com.ai.interview.service;

import org.springframework.stereotype.Service;

@Service
public class InterviewPromptService {

    public String buildPrompt(String question, String answer) {
        return "You are an expert interviewer.\n\n" +
                "Evaluate the candidate's answer.\n\n" +
                "Question:\n" + question + "\n\n" +
                "Candidate Answer:\n" + answer + "\n\n" +
                "Give response in this EXACT format:\n\n" +
                "Score: <0-100>\n\n" +
                "Feedback:\n" +
                "- Strengths: <what is good>\n" +
                "- Weaknesses: <what is missing or wrong>\n\n" +
                "Improved Answer:\n" +
                "<Rewrite the answer in a better professional way>";
    }
}
