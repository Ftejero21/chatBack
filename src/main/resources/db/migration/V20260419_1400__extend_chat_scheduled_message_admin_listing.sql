ALTER TABLE chat_scheduled_message
    ADD COLUMN admin_payload TEXT NULL,
    ADD COLUMN canceled_by BIGINT NULL,
    ADD COLUMN canceled_at TIMESTAMP NULL,
    ADD CONSTRAINT fk_sched_canceled_by FOREIGN KEY (canceled_by) REFERENCES usuarios(id);
