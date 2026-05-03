package com.chat.chat.Configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StickersMensajesEsquemaFix {

    private static final Logger LOGGER = LoggerFactory.getLogger(StickersMensajesEsquemaFix.class);
    private static final String SQL_COLUMN_EXISTS = "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
    private static final String SQL_INDEX_EXISTS = "SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
    private static final String SQL_FK_EXISTS = "SELECT COUNT(1) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ? AND CONSTRAINT_TYPE = 'FOREIGN KEY'";
    private static final String SQL_ADD_MENSAJE_STICKER_ID = "ALTER TABLE mensajes ADD COLUMN sticker_id BIGINT NULL";
    private static final String SQL_ADD_MENSAJE_STICKER_IDX = "CREATE INDEX idx_mensajes_sticker_id ON mensajes (sticker_id)";
    private static final String SQL_ADD_MENSAJE_STICKER_FK = "ALTER TABLE mensajes ADD CONSTRAINT fk_mensajes_sticker_id FOREIGN KEY (sticker_id) REFERENCES stickers(id)";
    private static final String SQL_ADD_STICKER_SOURCE_ID = "ALTER TABLE stickers ADD COLUMN source_sticker_id BIGINT NULL";
    private static final String SQL_ADD_STICKER_SOURCE_IDX = "CREATE INDEX idx_stickers_user_source_active ON stickers (usuario_id, source_sticker_id, activo)";
    private static final String SQL_ADD_STICKER_SOURCE_FK = "ALTER TABLE stickers ADD CONSTRAINT fk_stickers_source_sticker_id FOREIGN KEY (source_sticker_id) REFERENCES stickers(id)";

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.db.fix-stickers-mensajes-on-startup:true}")
    private boolean habilitado;

    public StickersMensajesEsquemaFix(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void asegurarEsquema() {
        if (!habilitado) {
            return;
        }
        try {
            addColumnIfMissing("mensajes", "sticker_id", SQL_ADD_MENSAJE_STICKER_ID);
            addIndexIfMissing("mensajes", "idx_mensajes_sticker_id", SQL_ADD_MENSAJE_STICKER_IDX);
            addFkIfMissing("mensajes", "fk_mensajes_sticker_id", SQL_ADD_MENSAJE_STICKER_FK);
            addColumnIfMissing("stickers", "source_sticker_id", SQL_ADD_STICKER_SOURCE_ID);
            addIndexIfMissing("stickers", "idx_stickers_user_source_active", SQL_ADD_STICKER_SOURCE_IDX);
            addFkIfMissing("stickers", "fk_stickers_source_sticker_id", SQL_ADD_STICKER_SOURCE_FK);
        } catch (Exception ex) {
            LOGGER.warn("[DB_FIX] no se pudo asegurar esquema stickers/mensajes: {}", ex.getClass().getSimpleName());
        }
    }

    private void addColumnIfMissing(String table, String column, String sql) {
        Integer count = jdbcTemplate.queryForObject(SQL_COLUMN_EXISTS, Integer.class, table, column);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(sql);
        LOGGER.info("[DB_FIX] columna {}.{} creada", table, column);
    }

    private void addIndexIfMissing(String table, String index, String sql) {
        Integer count = jdbcTemplate.queryForObject(SQL_INDEX_EXISTS, Integer.class, table, index);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(sql);
        LOGGER.info("[DB_FIX] indice {} creado", index);
    }

    private void addFkIfMissing(String table, String fkName, String sql) {
        Integer count = jdbcTemplate.queryForObject(SQL_FK_EXISTS, Integer.class, table, fkName);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(sql);
        LOGGER.info("[DB_FIX] fk {} creada", fkName);
    }
}
