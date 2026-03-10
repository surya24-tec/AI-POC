package com.ai.interview.service;

import org.springframework.stereotype.Service;

@Service
public class InterviewPromptService {

    public String buildPrompt(String question, String answer) {
        return "You are an expert technical interviewer and communication coach.\n" +
                "Evaluate the following user answer to the interview question.\n\n" +
                "Question: " + question + "\n" +
                "User Answer: " + answer + "\n\n" +
                "Provide your evaluation strictly in the following format. Ensure that the 'Feedback' is exactly 2 lines long, and the 'Improved Answer' is 4-5 lines long and highly professional:\n\n"
                +
                "Feedback: <feedback about the answer in exactly 2 lines>\n\n" +
                "Improved Answer: <professional improved answer in 4-5 lines>";
    }
}
