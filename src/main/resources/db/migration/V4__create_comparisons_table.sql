-- Create comparisons table
-- Stores user queries with AI-generated repository comparisons
CREATE TABLE comparisons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    session_id VARCHAR(255),
    user_query TEXT NOT NULL,
    normalized_query VARCHAR(255),
    technologies VARCHAR(255),
    problem_domains VARCHAR(255),
    architecture_patterns VARCHAR(255),
    constraints JSON DEFAULT ('[]'),
    github_search_query TEXT,
    repos_compared_count INT,
    recommended_repo_full_name VARCHAR(255),
    recommendation_reasoning TEXT,
    ranking_results JSON,
    model_used VARCHAR(255),
    input_tokens INT,
    output_tokens INT,
    cost_usd DECIMAL(10, 6),
    status VARCHAR(255),
    view_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_comparisons_session (session_id),
    KEY idx_comparisons_user (user_id),
    KEY idx_comparisons_created_at (created_at),
    KEY idx_comparisons_problem_domains (problem_domains),
    KEY idx_comparisons_view_count (view_count),
    KEY idx_comparisons_user_created (user_id, created_at),

    CONSTRAINT fk_comparisons_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
