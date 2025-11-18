-- Repository Categories table (join table: repositories <-> categories)
CREATE TABLE repository_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    confidence_score FLOAT,
    assigned_by VARCHAR(255) DEFAULT 'ai' COMMENT 'ai, manual, github_topics',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_repo_categories_unique (repository_id, category_id),
    KEY idx_repo_categories_repository (repository_id),
    KEY idx_repo_categories_category (category_id),
    KEY idx_repo_categories_confidence (confidence_score),

    CONSTRAINT fk_repo_categories_repository FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    CONSTRAINT fk_repo_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
