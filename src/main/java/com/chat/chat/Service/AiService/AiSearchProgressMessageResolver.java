package com.chat.chat.Service.AiService;

import com.chat.chat.Utils.AiSearchProgressStep;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AiSearchProgressMessageResolver {

    public String resolve(AiSearchProgressStep step,
                          String status,
                          String target,
                          String tipoMensajeSolicitado,
                          String complaintDirection) {
        String normalizedTarget = normalize(target);
        String normalizedTipo = normalize(tipoMensajeSolicitado);
        String normalizedDirection = normalizeComplaintDirection(complaintDirection);
        boolean complaints = "COMPLAINTS_RECEIVED".equals(normalizedTarget) || "COMPLAINTS_CREATED".equals(normalizedTarget) || "MIXED".equals(normalizedTarget);

        if (complaints) {
            return resolveComplaints(step, status, normalizedDirection);
        }
        if ("MESSAGES".equals(normalizedTarget) || normalizedTarget == null) {
            return resolveMessages(step, status, normalizedTipo);
        }
        return resolveDefault(step, status);
    }

    private String resolveComplaints(AiSearchProgressStep step, String status, String complaintDirection) {
        return switch (step) {
            case ANALYZING_CONTEXT -> "STARTED".equals(status) ? "Analizando denuncias..." : "Denuncias analizadas";
            case ANALYZING_MESSAGES -> "STARTED".equals(status) ? "Analizando denuncias..." : "Denuncias analizadas";
            case COMPLAINTS_SEARCH -> switch (status) {
                case "STARTED" -> switch (complaintDirection) {
                    case "RECEIVED" -> "Buscando denuncias recibidas...";
                    case "CREATED" -> "Buscando denuncias realizadas...";
                    default -> "Buscando denuncias...";
                };
                case "FAILED" -> "No se pudieron consultar las denuncias";
                default -> "Busqueda de denuncias finalizada";
            };
            case MESSAGE_FOUND -> "Denuncia encontrada";
            case MESSAGE_NOT_FOUND -> "No se encontraron denuncias para este usuario";
            case ERROR -> "Error al procesar la busqueda de denuncias";
            case APP_REPORT -> "STARTED".equals(status) ? "Generando reporte..." : "Reporte generado";
            case APP_REPORT_STATUS -> "STARTED".equals(status) ? "Buscando tus reportes..." : "Busqueda de reportes finalizada";
        };
    }

    private String resolveMessages(AiSearchProgressStep step, String status, String tipo) {
        boolean image = "IMAGE".equals(tipo);
        boolean audio = "AUDIO".equals(tipo);
        boolean sticker = "STICKER".equals(tipo);
        boolean file = "FILE".equals(tipo);
        boolean specialized = image || audio || sticker || file;
        return switch (step) {
            case ANALYZING_CONTEXT -> {
                if (!specialized) yield "STARTED".equals(status) ? "Analizando contexto..." : "Contexto analizado";
                if (image) yield "STARTED".equals(status) ? "Analizando contexto de imagenes..." : "Contexto de imagenes analizado";
                if (audio) yield "STARTED".equals(status) ? "Analizando contexto de audios..." : "Contexto de audios analizado";
                if (sticker) yield "STARTED".equals(status) ? "Analizando contexto de stickers..." : "Contexto de stickers analizado";
                yield "STARTED".equals(status) ? "Analizando contexto de archivos..." : "Contexto de archivos analizado";
            }
            case ANALYZING_MESSAGES -> {
                if (!specialized) yield "STARTED".equals(status) ? "Analizando mensajes..." : "Mensajes analizados";
                if (image) yield "STARTED".equals(status) ? "Buscando imagenes..." : "Imagenes analizadas";
                if (audio) yield "STARTED".equals(status) ? "Buscando audios..." : "Audios analizados";
                if (sticker) yield "STARTED".equals(status) ? "Buscando stickers..." : "Stickers analizados";
                yield "STARTED".equals(status) ? "Buscando archivos..." : "Archivos analizados";
            }
            case MESSAGE_FOUND -> {
                if (image) yield "Imagen encontrada";
                if (audio) yield "Audio encontrado";
                if (sticker) yield "Sticker encontrado";
                if (file) yield "Archivo encontrado";
                yield "Mensaje encontrado";
            }
            case MESSAGE_NOT_FOUND -> {
                if (image) yield "No se encontraron imagenes";
                if (audio) yield "No se encontraron audios";
                if (sticker) yield "No se encontraron stickers";
                if (file) yield "No se encontraron archivos";
                yield "No se encontro una coincidencia clara";
            }
            case ERROR -> {
                if (image) yield "Error al procesar la busqueda de imagenes";
                if (audio) yield "Error al procesar la busqueda de audios";
                if (sticker) yield "Error al procesar la busqueda de stickers";
                if (file) yield "Error al procesar la busqueda de archivos";
                yield "Error al procesar la busqueda";
            }
            case COMPLAINTS_SEARCH -> "STARTED".equals(status) ? "Buscando denuncias..." : "Busqueda de denuncias finalizada";
            case APP_REPORT -> "STARTED".equals(status) ? "Generando reporte..." : "Reporte generado";
            case APP_REPORT_STATUS -> "STARTED".equals(status) ? "Buscando tus reportes..." : "Busqueda de reportes finalizada";
        };
    }

    private String resolveDefault(AiSearchProgressStep step, String status) {
        return switch (step) {
            case ANALYZING_CONTEXT -> "STARTED".equals(status) ? "Analizando contexto..." : "Contexto analizado";
            case ANALYZING_MESSAGES -> "STARTED".equals(status) ? "Analizando mensajes..." : "Mensajes analizados";
            case COMPLAINTS_SEARCH -> "STARTED".equals(status) ? "Buscando denuncias..." : "Busqueda de denuncias finalizada";
            case MESSAGE_FOUND -> "Mensaje encontrado";
            case MESSAGE_NOT_FOUND -> "No se encontro una coincidencia clara";
            case APP_REPORT -> "STARTED".equals(status) ? "Generando reporte..." : "Reporte generado";
            case APP_REPORT_STATUS -> "STARTED".equals(status) ? "Buscando tus reportes..." : "Busqueda de reportes finalizada";
            case ERROR -> "Error al procesar la busqueda";
        };
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeComplaintDirection(String value) {
        String normalized = normalize(value);
        if ("COMPLAINT_RECEIVED".equals(normalized)) return "RECEIVED";
        if ("COMPLAINT_CREATED".equals(normalized)) return "CREATED";
        if ("RECEIVED".equals(normalized) || "CREATED".equals(normalized)) return normalized;
        return "ANY";
    }
}
