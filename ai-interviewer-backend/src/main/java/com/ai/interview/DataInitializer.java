package com.ai.interview;

import com.ai.interview.model.Question;
import com.ai.interview.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(QuestionRepository repository) {
        return args -> {
            // Seed DB with professional interview questions (used for first 3 questions)
            if (repository.count() == 0) {
                repository.saveAll(Arrays.asList(
                        new Question(null, "Tell me about yourself and your professional background.", "Introduction", false),
                        new Question(null, "Explain a challenging project you worked on recently.", "Experience", false),
                        new Question(null, "What are your strengths and weaknesses?", "Personality", false),
                        new Question(null, "Why should we hire you for this position?", "Interview", false),
                        new Question(null, "Where do you see yourself in the next 3-5 years?", "Goals", false),
                        new Question(null, "How do you handle pressure and tight deadlines?", "Situational", false)));
                System.out.println(">>> DB seeded with 6 interview questions.");
            }
        };
    }
}
