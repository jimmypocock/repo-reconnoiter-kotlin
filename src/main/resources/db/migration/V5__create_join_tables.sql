-- Create join tables for many-to-many relationships

-- Repository Categories (repositories <-> categories)
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

-- Comparison Categories (comparisons <-> categories)
CREATE TABLE comparison_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comparison_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    confidence_score DECIMAL(3, 2),
    assigned_by VARCHAR(255) DEFAULT 'inferred',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_comparison_categories_unique (comparison_id, category_id),
    KEY idx_comparison_categories_comparison (comparison_id),
    KEY idx_comparison_categories_category (category_id),

    CONSTRAINT fk_comparison_categories_comparison FOREIGN KEY (comparison_id) REFERENCES comparisons(id) ON DELETE CASCADE,
    CONSTRAINT fk_comparison_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comparison Repositories (comparisons <-> repositories with ranking)
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
