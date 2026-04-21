CREATE TABLE user_moderation_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    description TEXT NULL,
    origin VARCHAR(80) NOT NULL,
    admin_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_moderation_history_user FOREIGN KEY (user_id) REFERENCES usuarios(id),
    CONSTRAINT fk_user_moderation_history_admin FOREIGN KEY (admin_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_user_moderation_history_user_created_at
    ON user_moderation_history (user_id, created_at);
