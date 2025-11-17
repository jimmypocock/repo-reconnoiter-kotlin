-- Create status tracking and queue management tables

-- Queued Analyses (batch processing queue)
CREATE TABLE queued_analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    analysis_type VARCHAR(255) NOT NULL COMMENT 'tier1_categorization, tier2_deep_analysis',
    status VARCHAR(255) DEFAULT 'pending' COMMENT 'pending, processing, completed, failed',
    priority INT DEFAULT 0,
    retry_count INT DEFAULT 0,
    scheduled_for TIMESTAMP NULL,
    processed_at TIMESTAMP NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_queued_analyses_repository (repository_id),
    KEY idx_queued_analyses_created_at (created_at),
    KEY idx_queued_analyses_processing (status, priority, scheduled_for),

    CONSTRAINT fk_queued_analyses_repository FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Analysis Statuses (async analysis tracking)
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

-- Comparison Statuses (async comparison creation tracking)
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
