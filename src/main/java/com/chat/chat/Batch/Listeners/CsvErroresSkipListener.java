package com.chat.chat.Batch.Listeners;

import com.chat.chat.DTO.UsuarioReporteCsvDTO;
import com.chat.chat.Entity.UsuarioEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Listener que guarda en un CSV los registros saltados (skips) en read/process/write.
 * Crea el fichero si no existe y añade cabecera la primera vez.
 */
public class CsvErroresSkipListener implements SkipListener<UsuarioEntity, UsuarioReporteCsvDTO> {

    private static final Logger log = LoggerFactory.getLogger(CsvErroresSkipListener.class);
    private static final String FASE_READ = "READ";
    private static final String FASE_PROCESS = "PROCESS";
    private static final String FASE_WRITE = "WRITE";
    private static final String EMPTY = "";
    private static final String DELIMITER = ";";
    private static final String HEADER = "FASE;ID;EMAIL;NOMBRE;ERROR_TIPO;ERROR_MENSAJE";
    private static final String LOG_ERROR_WRITE = "No se pudo escribir el log de errores en {}: {}";
    private static final String LF = "\n";
    private static final String CR = "\r";
    private static final String SPACE = " ";

    private final Path errorPath;
    private volatile boolean headerEscrito = false;

    public CsvErroresSkipListener(Path errorPath) {
        this.errorPath = errorPath;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        appendLinea(FASE_READ, null, null, null, t);
    }

    @Override
    public void onSkipInProcess(UsuarioEntity item, Throwable t) {
        String id = item != null ? String.valueOf(item.getId()) : EMPTY;
        String email = item != null ? nullSafe(item.getEmail()) : EMPTY;
        String nombre = item != null ? nullSafe(item.getNombre()) : EMPTY;
        appendLinea(FASE_PROCESS, id, email, nombre, t);
    }

    @Override
    public void onSkipInWrite(UsuarioReporteCsvDTO item, Throwable t) {
        String id = item != null ? String.valueOf(item.getId()) : EMPTY;
        String email = item != null ? nullSafe(item.getEmail()) : EMPTY;
        String nombre = item != null ? nullSafe(item.getNombre()) : EMPTY;
        appendLinea(FASE_WRITE, id, email, nombre, t);
    }

    private String nullSafe(String s) {
        return s == null ? EMPTY : s.trim();
    }

    private synchronized void appendLinea(String fase, String id, String email, String nombre, Throwable t) {
        try {
            if (!headerEscrito || !Files.exists(errorPath)) {
                // Crear padre y escribir cabecera
                if (errorPath.getParent() != null) {
                    Files.createDirectories(errorPath.getParent());
                }
                String header = HEADER;
                Files.writeString(errorPath, header + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                headerEscrito = true;
            }

            String tipo = t != null ? t.getClass().getSimpleName() : EMPTY;
            String msg = t != null ? sanitize(t.getMessage()) : EMPTY;
            String linea = String.join(DELIMITER, fase,
                    safe(id), safe(email), safe(nombre), safe(tipo), safe(msg));

            Files.writeString(errorPath, linea + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException io) {
            log.warn(LOG_ERROR_WRITE, errorPath, io.getMessage());
        }
    }

    private String sanitize(String s) {
        if (s == null) return EMPTY;
        // Evitar saltos de línea en CSV
        return s.replace(LF, SPACE).replace(CR, SPACE).trim();
    }

    private String safe(String s) {
        return s == null ? EMPTY : s;
    }
}
