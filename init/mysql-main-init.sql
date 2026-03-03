-- Init script for mysql-main
-- Creates user_db and product_db on the same MySQL instance

CREATE DATABASE IF NOT EXISTS user_db;
CREATE DATABASE IF NOT EXISTS product_db;

-- Grant permissions
GRANT ALL PRIVILEGES ON user_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON product_db.* TO 'root'@'%';
FLUSH PRIVILEGES;
