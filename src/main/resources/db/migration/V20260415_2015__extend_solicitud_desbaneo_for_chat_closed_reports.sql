ALTER TABLE solicitud_desbaneo
    ADD COLUMN IF NOT EXISTS tipo_reporte VARCHAR(20) NOT NULL DEFAULT 'DESBANEO',
    ADD COLUMN IF NOT EXISTS chat_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS chat_nombre_snapshot VARCHAR(190) NULL,
    ADD COLUMN IF NOT EXISTS chat_cerrado_motivo_snapshot VARCHAR(500) NULL;

CREATE INDEX IF NOT EXISTS idx_solicitud_desbaneo_tipo_chat_usuario_estado
    ON solicitud_desbaneo (tipo_reporte, chat_id, usuario_id, estado);
