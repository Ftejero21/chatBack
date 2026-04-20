CREATE TABLE user_complaint (
    id BIGINT NOT NULL AUTO_INCREMENT,
    denunciante_id BIGINT NOT NULL,
    denunciado_id BIGINT NOT NULL,
    chat_id BIGINT NULL,
    motivo VARCHAR(120) NOT NULL,
    detalle TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    leida BIT(1) NOT NULL DEFAULT b'0',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leida_at TIMESTAMP NULL,
    denunciante_nombre VARCHAR(190) NULL,
    denunciado_nombre VARCHAR(190) NULL,
    chat_nombre_snapshot VARCHAR(190) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_complaint_denunciante FOREIGN KEY (denunciante_id) REFERENCES usuarios(id),
    CONSTRAINT fk_user_complaint_denunciado FOREIGN KEY (denunciado_id) REFERENCES usuarios(id),
    CONSTRAINT fk_user_complaint_chat FOREIGN KEY (chat_id) REFERENCES chats(id)
);

CREATE INDEX idx_user_complaint_created_at ON user_complaint (created_at);
CREATE INDEX idx_user_complaint_leida_created_at ON user_complaint (leida, created_at);
CREATE INDEX idx_user_complaint_estado_created_at ON user_complaint (estado, created_at);
CREATE INDEX idx_user_complaint_denunciado_created_at ON user_complaint (denunciado_id, created_at);
CREATE INDEX idx_user_complaint_denunciante_created_at ON user_complaint (denunciante_id, created_at);
