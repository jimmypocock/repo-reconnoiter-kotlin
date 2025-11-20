-- Add FULLTEXT index for comparison search with n-gram parser
-- Enables fast MySQL FULLTEXT search across query and metadata fields
-- Uses n-gram parser for better matching of short technical terms (api, db, job, etc.)

-- Create FULLTEXT index with n-gram parser on searchable text columns
CREATE FULLTEXT INDEX idx_comparisons_fulltext_search
ON comparisons(user_query, normalized_query, technologies, problem_domains, architecture_patterns)
WITH PARSER ngram;

-- Note: MySQL n-gram FULLTEXT search:
-- - Default ngram_token_size: 2 characters (configurable globally)
-- - Tokenizes text into character sequences for better partial matching
-- - Supports short technical terms: "api", "db", "job", "orm", "js", "py", etc.
-- - Boolean mode allows wildcard searches: MATCH(...) AGAINST('+job* +queue*' IN BOOLEAN MODE)
-- - Natural language mode provides relevance scoring: MATCH(...) AGAINST('...' IN NATURAL LANGUAGE MODE)
-- - Better for CJK languages and partial substring matching than default parser
