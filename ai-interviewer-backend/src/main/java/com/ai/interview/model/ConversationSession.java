package com.ai.interview.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Integer score;
    private String feedback;

    @Enumerated(EnumType.STRING)
    private SessionStatus status; // ACTIVE, COMPLETED

    public enum SessionStatus {
        ACTIVE, COMPLETED
    }
}
