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

    private final Path errorPath;
    private volatile boolean headerEscrito = false;

    public CsvErroresSkipListener(Path errorPath) {
        this.errorPath = errorPath;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        appendLinea("READ", null, null, null, t);
    }

    @Override
    public void onSkipInProcess(UsuarioEntity item, Throwable t) {
        String id = item != null ? String.valueOf(item.getId()) : "";
        String email = item != null ? nullSafe(item.getEmail()) : "";
        String nombre = item != null ? nullSafe(item.getNombre()) : "";
        appendLinea("PROCESS", id, email, nombre, t);
    }

    @Override
    public void onSkipInWrite(UsuarioReporteCsvDTO item, Throwable t) {
        String id = item != null ? String.valueOf(item.getId()) : "";
        String email = item != null ? nullSafe(item.getEmail()) : "";
        String nombre = item != null ? nullSafe(item.getNombre()) : "";
        appendLinea("WRITE", id, email, nombre, t);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s.trim();
    }

    private synchronized void appendLinea(String fase, String id, String email, String nombre, Throwable t) {
        try {
            if (!headerEscrito || !Files.exists(errorPath)) {
                // Crear padre y escribir cabecera
                if (errorPath.getParent() != null) {
                    Files.createDirectories(errorPath.getParent());
                }
                String header = "FASE;ID;EMAIL;NOMBRE;ERROR_TIPO;ERROR_MENSAJE";
                Files.writeString(errorPath, header + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                headerEscrito = true;
            }

            String tipo = t != null ? t.getClass().getSimpleName() : "";
            String msg = t != null ? sanitize(t.getMessage()) : "";
            String linea = String.join(";", fase,
                    safe(id), safe(email), safe(nombre), safe(tipo), safe(msg));

            Files.writeString(errorPath, linea + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException io) {
            log.warn("No se pudo escribir el log de errores en {}: {}", errorPath, io.getMessage());
        }
    }

    private String sanitize(String s) {
        if (s == null) return "";
        // Evitar saltos de línea en CSV
        return s.replace("\n", " ").replace("\r", " ").trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
