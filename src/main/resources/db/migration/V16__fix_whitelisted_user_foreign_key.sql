-- Fix foreign key constraint on users.whitelisted_user_id
-- Allow deletion of whitelisted users by setting reference to NULL instead of blocking deletion

-- Drop the existing foreign key constraint
ALTER TABLE users
DROP FOREIGN KEY fk_users_whitelisted_user;

-- Re-add the foreign key constraint with ON DELETE SET NULL
ALTER TABLE users
ADD CONSTRAINT fk_users_whitelisted_user
FOREIGN KEY (whitelisted_user_id)
REFERENCES whitelisted_users(id)
ON DELETE SET NULL;
