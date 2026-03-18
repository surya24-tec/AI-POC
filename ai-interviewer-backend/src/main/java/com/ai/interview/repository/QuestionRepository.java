package com.ai.interview.repository;

import com.ai.interview.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByIsAiGenerated(boolean isAiGenerated);
}
