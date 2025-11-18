-- Comparison Categories table (join table: comparisons <-> categories)
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
