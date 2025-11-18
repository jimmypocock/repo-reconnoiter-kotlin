-- Queued Analyses table (batch processing queue)
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
