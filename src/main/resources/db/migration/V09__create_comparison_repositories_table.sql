-- Comparison Repositories table (join table: comparisons <-> repositories with ranking)
CREATE TABLE comparison_repositories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comparison_id BIGINT NOT NULL,
    repository_id BIGINT NOT NULL,
    ranking INT,
    score INT,
    fit_reasoning TEXT,
    pros JSON DEFAULT ('[]'),
    cons JSON DEFAULT ('[]'),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_comparison_repositories_comparison (comparison_id),
    KEY idx_comparison_repositories_repository (repository_id),
    KEY idx_comparison_repositories_ranking (comparison_id, ranking),

    CONSTRAINT fk_comparison_repositories_comparison FOREIGN KEY (comparison_id) REFERENCES comparisons(id) ON DELETE CASCADE,
    CONSTRAINT fk_comparison_repositories_repository FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
