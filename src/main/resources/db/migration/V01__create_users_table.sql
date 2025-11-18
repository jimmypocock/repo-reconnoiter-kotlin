-- Users table (authentication via GitHub OAuth)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    github_id BIGINT UNIQUE,
    github_username VARCHAR(255),
    github_name VARCHAR(255),
    github_avatar_url VARCHAR(255),
    provider VARCHAR(255),
    uid VARCHAR(255),
    admin BOOLEAN NOT NULL DEFAULT FALSE,
    encrypted_password VARCHAR(255) NOT NULL DEFAULT '',
    reset_password_token VARCHAR(255),
    reset_password_sent_at TIMESTAMP,
    remember_created_at TIMESTAMP,
    deleted_at TIMESTAMP,
    whitelisted_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_email (email),
    UNIQUE KEY idx_github_id (github_id),
    UNIQUE KEY idx_provider_uid (provider, uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
