-- Create base tables that other tables depend on

-- Users table
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

-- Whitelisted Users table
CREATE TABLE whitelisted_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    github_id BIGINT NOT NULL UNIQUE,
    github_username VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_whitelisted_github_id (github_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Repositories table
CREATE TABLE repositories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    github_id BIGINT NOT NULL UNIQUE,
    node_id VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    html_url VARCHAR(255) NOT NULL,
    description TEXT,
    owner_login VARCHAR(255),
    owner_avatar_url VARCHAR(255),
    owner_type VARCHAR(255),
    language VARCHAR(255),
    stargazers_count INT NOT NULL DEFAULT 0,
    forks_count INT NOT NULL DEFAULT 0,
    watchers_count INT NOT NULL DEFAULT 0,
    open_issues_count INT NOT NULL DEFAULT 0,
    topics JSON DEFAULT ('[]'),
    homepage_url VARCHAR(255),
    license VARCHAR(255),
    default_branch VARCHAR(255) NOT NULL DEFAULT 'main',
    is_fork BOOLEAN NOT NULL DEFAULT FALSE,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    disabled BOOLEAN NOT NULL DEFAULT FALSE,
    visibility VARCHAR(255) NOT NULL DEFAULT 'public',
    clone_url VARCHAR(255),
    size INT,
    search_score DOUBLE,
    github_created_at TIMESTAMP,
    github_updated_at TIMESTAMP,
    github_pushed_at TIMESTAMP,
    readme_content TEXT,
    readme_sha VARCHAR(255),
    readme_length INT,
    readme_fetched_at TIMESTAMP,
    last_analyzed_at TIMESTAMP,
    last_fetched_at TIMESTAMP,
    fetch_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_github_id (github_id),
    KEY idx_node_id (node_id),
    KEY idx_full_name (full_name),
    KEY idx_stargazers_count (stargazers_count),
    KEY idx_language (language),
    KEY idx_github_created_at (github_created_at),
    KEY idx_github_pushed_at (github_pushed_at),
    KEY idx_last_analyzed_at (last_analyzed_at),
    KEY idx_archived_disabled (archived, disabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
