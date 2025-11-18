-- AI Costs table (daily rollup of AI API spending)
CREATE TABLE ai_costs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    model_used VARCHAR(255) NOT NULL,
    user_id BIGINT,
    total_cost_usd DECIMAL(10, 6) DEFAULT 0.0,
    total_input_tokens BIGINT DEFAULT 0,
    total_output_tokens BIGINT DEFAULT 0,
    total_requests INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_ai_costs_date_model (date, model_used),
    KEY idx_ai_costs_date (date),
    KEY idx_ai_costs_user (user_id),

    CONSTRAINT fk_ai_costs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
