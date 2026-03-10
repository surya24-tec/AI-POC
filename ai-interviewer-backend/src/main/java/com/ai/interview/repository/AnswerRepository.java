package com.ai.interview.repository;

import com.ai.interview.model.Answer;
import com.ai.interview.model.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findBySession(ConversationSession session);
}
