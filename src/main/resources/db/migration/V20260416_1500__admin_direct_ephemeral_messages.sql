ALTER TABLE chats_individuales
    ADD COLUMN admin_direct BIT(1) NOT NULL DEFAULT b'0';

ALTER TABLE mensajes
    ADD COLUMN admin_message BIT(1) NOT NULL DEFAULT b'0',
    ADD COLUMN expires_after_read_seconds BIGINT NULL,
    ADD COLUMN first_read_at DATETIME NULL,
    ADD COLUMN expire_at DATETIME NULL,
    ADD COLUMN expired_by_policy BIT(1) NOT NULL DEFAULT b'0';

UPDATE mensajes
SET expire_at = expira_en
WHERE expire_at IS NULL
  AND expira_en IS NOT NULL;

CREATE INDEX idx_chats_individuales_admin_direct_users
    ON chats_individuales (admin_direct, usuario1_id, usuario2_id);

CREATE INDEX idx_mensajes_admin_expire_at
    ON mensajes (admin_message, expire_at);
