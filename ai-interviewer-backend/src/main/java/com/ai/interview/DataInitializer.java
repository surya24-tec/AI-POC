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
            if (repository.count() == 0) {
                repository.saveAll(Arrays.asList(
                        new Question(null, "Introduce yourself to the team.", "Communication", false),
                        new Question(null, "Describe a difficult situation you handled at work or school.",
                                "Situational", false),
                        new Question(null, "Why should we hire you for this position?", "Interview", false),
                        new Question(null, "Explain a project you worked on recently.", "Experience", false),
                        new Question(null, "What are your strengths and weaknesses?", "Personality", false),
                        new Question(null, "Where do you see yourself in five years?", "Goals", false)));
            }
        };
    }
}
