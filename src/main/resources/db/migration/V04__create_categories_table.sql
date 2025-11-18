-- Categories table (taxonomy for repository classification)
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    category_type VARCHAR(255) NOT NULL COMMENT 'problem_domain, architecture_pattern, maturity_level',
    repositories_count INT DEFAULT 0,
    embedding JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_slug_category_type (slug, category_type),
    KEY idx_category_type (category_type),
    KEY idx_category_type_repositories_count (category_type, repositories_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
