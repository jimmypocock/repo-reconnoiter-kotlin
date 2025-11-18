-- Comparison Statuses table (async comparison creation tracking)
CREATE TABLE comparison_statuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    comparison_id BIGINT,
    status VARCHAR(255) NOT NULL DEFAULT 'processing' COMMENT 'processing, completed, failed',
    error_message TEXT,
    pending_cost_usd DECIMAL(10, 6) NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_comparison_statuses_session (session_id),
    KEY idx_comparison_statuses_user (user_id),
    KEY idx_comparison_statuses_comparison (comparison_id),

    CONSTRAINT fk_comparison_statuses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comparison_statuses_comparison FOREIGN KEY (comparison_id) REFERENCES comparisons(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
