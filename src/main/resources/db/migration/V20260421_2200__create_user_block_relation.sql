CREATE TABLE user_block_relation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_block_relation_blocker FOREIGN KEY (blocker_id) REFERENCES usuarios(id),
    CONSTRAINT fk_user_block_relation_blocked FOREIGN KEY (blocked_id) REFERENCES usuarios(id),
    CONSTRAINT uk_user_block_relation_pair UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX idx_user_block_relation_blocker ON user_block_relation (blocker_id);
CREATE INDEX idx_user_block_relation_blocked ON user_block_relation (blocked_id);

INSERT INTO user_block_relation (blocker_id, blocked_id, source, created_at, updated_at)
SELECT ub.usuario_id, ub.bloqueado_id, 'MANUAL', UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM usuario_bloqueados ub
LEFT JOIN user_block_relation ubr
    ON ubr.blocker_id = ub.usuario_id AND ubr.blocked_id = ub.bloqueado_id
WHERE ubr.id IS NULL;
