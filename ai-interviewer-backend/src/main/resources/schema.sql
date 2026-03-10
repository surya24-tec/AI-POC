CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT,
    category VARCHAR(255),
    is_ai_generated BOOLEAN
);

CREATE TABLE IF NOT EXISTS conversation_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    start_time DATETIME,
    end_time DATETIME,
    score INT,
    feedback TEXT,
    status VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT,
    question_id BIGINT,
    content TEXT,
    timestamp DATETIME,
    sub_score INT,
    FOREIGN KEY (session_id) REFERENCES conversation_sessions(id),
    FOREIGN KEY (question_id) REFERENCES questions(id)
);
