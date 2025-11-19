-- Whitelisted Users table (invite-only access control)
CREATE TABLE whitelisted_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    github_id BIGINT NOT NULL UNIQUE,
    github_username VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    notes VARCHAR(255),
    added_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_whitelisted_github_id (github_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
