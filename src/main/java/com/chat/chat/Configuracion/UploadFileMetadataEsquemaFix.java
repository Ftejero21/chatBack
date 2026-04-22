package com.chat.chat.Configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UploadFileMetadataEsquemaFix {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadFileMetadataEsquemaFix.class);

    private static final String SQL_CREATE_TABLE_UPLOAD_METADATA = """
            CREATE TABLE IF NOT EXISTS upload_file_metadata (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              owner_user_id BIGINT NOT NULL,
              chat_id BIGINT NULL,
              message_id BIGINT NULL,
              tipo VARCHAR(20) NOT NULL,
              public_url VARCHAR(1024) NOT NULL UNIQUE,
              storage_path VARCHAR(1024) NOT NULL,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              CONSTRAINT fk_upload_meta_owner FOREIGN KEY (owner_user_id) REFERENCES usuarios(id) ON DELETE CASCADE,
              CONSTRAINT fk_upload_meta_chat FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE SET NULL,
              CONSTRAINT fk_upload_meta_message FOREIGN KEY (message_id) REFERENCES mensajes(id) ON DELETE SET NULL,
              INDEX idx_upload_meta_owner (owner_user_id),
              INDEX idx_upload_meta_chat (chat_id),
              INDEX idx_upload_meta_message (message_id),
              INDEX idx_upload_meta_tipo (tipo)
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public UploadFileMetadataEsquemaFix(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void asegurarEsquemaUploadMetadata() {
        try {
            jdbcTemplate.execute(SQL_CREATE_TABLE_UPLOAD_METADATA);
            LOGGER.info("[DB_FIX] esquema de upload_file_metadata verificado");
        } catch (Exception ex) {
            LOGGER.warn("[DB_FIX] no se pudo verificar esquema de upload_file_metadata: {}", ex.getClass().getSimpleName());
        }
    }
}
