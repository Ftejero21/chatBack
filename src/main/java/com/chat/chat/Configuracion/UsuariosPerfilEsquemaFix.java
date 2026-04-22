package com.chat.chat.Configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UsuariosPerfilEsquemaFix {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsuariosPerfilEsquemaFix.class);

    private static final String SQL_COLUMN_EXISTS = "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = ?";
    private static final String SQL_ADD_DNI = "ALTER TABLE usuarios ADD COLUMN dni VARCHAR(16) NULL";
    private static final String SQL_ADD_TELEFONO = "ALTER TABLE usuarios ADD COLUMN telefono VARCHAR(32) NULL";
    private static final String SQL_ADD_FECHA_NACIMIENTO = "ALTER TABLE usuarios ADD COLUMN fecha_nacimiento VARCHAR(32) NULL";
    private static final String SQL_ADD_GENERO = "ALTER TABLE usuarios ADD COLUMN genero VARCHAR(32) NULL";
    private static final String SQL_ADD_DIRECCION = "ALTER TABLE usuarios ADD COLUMN direccion VARCHAR(255) NULL";
    private static final String SQL_ADD_NACIONALIDAD = "ALTER TABLE usuarios ADD COLUMN nacionalidad VARCHAR(80) NULL";
    private static final String SQL_ADD_OCUPACION = "ALTER TABLE usuarios ADD COLUMN ocupacion VARCHAR(120) NULL";
    private static final String SQL_ADD_INSTAGRAM = "ALTER TABLE usuarios ADD COLUMN instagram VARCHAR(120) NULL";

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.db.fix-usuarios-perfil-on-startup:true}")
    private boolean habilitado;

    public UsuariosPerfilEsquemaFix(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void asegurarColumnasPerfilUsuario() {
        if (!habilitado) {
            return;
        }
        try {
            agregarColumnaSiNoExiste("dni", SQL_ADD_DNI);
            agregarColumnaSiNoExiste("telefono", SQL_ADD_TELEFONO);
            agregarColumnaSiNoExiste("fecha_nacimiento", SQL_ADD_FECHA_NACIMIENTO);
            agregarColumnaSiNoExiste("genero", SQL_ADD_GENERO);
            agregarColumnaSiNoExiste("direccion", SQL_ADD_DIRECCION);
            agregarColumnaSiNoExiste("nacionalidad", SQL_ADD_NACIONALIDAD);
            agregarColumnaSiNoExiste("ocupacion", SQL_ADD_OCUPACION);
            agregarColumnaSiNoExiste("instagram", SQL_ADD_INSTAGRAM);
            LOGGER.info("[DB_FIX] columnas de perfil de usuarios verificadas");
        } catch (Exception ex) {
            LOGGER.warn("[DB_FIX] no se pudo asegurar columnas de perfil en usuarios: {}", ex.getClass().getSimpleName());
        }
    }

    private void agregarColumnaSiNoExiste(String columna, String sqlAdd) {
        Integer count = jdbcTemplate.queryForObject(SQL_COLUMN_EXISTS, Integer.class, columna);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(sqlAdd);
        LOGGER.info("[DB_FIX] columna {} creada en tabla usuarios", columna);
    }
}
