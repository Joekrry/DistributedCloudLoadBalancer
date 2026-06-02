CREATE DATABASE IF NOT EXISTS cloudbalancer;
USE cloudbalancer;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    role ENUM('standard', 'admin') DEFAULT 'standard',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE files (
    id INT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    owner_id INT NOT NULL,
    file_size BIGINT,
    total_chunks INT,
    encryption_key_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE file_chunks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT NOT NULL,
    chunk_index INT NOT NULL,
    container_name VARCHAR(100) NOT NULL,
    chunk_path VARCHAR(500) NOT NULL,
    crc32_checksum BIGINT NOT NULL,
    chunk_size BIGINT NOT NULL,
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    UNIQUE(file_id, chunk_index)
);

CREATE TABLE file_permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT NOT NULL,
    user_id INT NOT NULL,
    permission ENUM('read', 'write') NOT NULL,
    granted_by INT NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES users(id),
    UNIQUE(file_id, user_id, permission)
);

CREATE TABLE event_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- placeholder admin; UserDAO replaces the hash/salt with a real PBKDF2 value
INSERT INTO users (username, password_hash, salt, role)
VALUES ('admin', 'PLACEHOLDER_HASH', 'PLACEHOLDER_SALT', 'admin');
