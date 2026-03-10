package com.ai.interview.repository;

import com.ai.interview.model.ConversationSession;
import com.ai.interview.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {
    List<ConversationSession> findByUser(User user);

    List<ConversationSession> findByStatus(ConversationSession.SessionStatus status);
}
