-- Analysis Statuses table (async analysis tracking)
CREATE TABLE analysis_statuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255),
    user_id BIGINT NOT NULL,
    repository_id BIGINT,
    status VARCHAR(255) COMMENT 'processing, completed, failed',
    error_message TEXT,
    pending_cost_usd DECIMAL(10, 6) NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_analysis_statuses_session (session_id),
    KEY idx_analysis_statuses_user (user_id),
    KEY idx_analysis_statuses_repository (repository_id),
    KEY idx_analysis_statuses_status_created (status, created_at),

    CONSTRAINT fk_analysis_statuses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_analysis_statuses_repository FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
