package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.Configuracion.DeepSeekProperties;
import com.chat.chat.DTO.AiReportAnalysisRequestDTO;
import com.chat.chat.DTO.AiReportAnalysisResponseDTO;
import com.chat.chat.DTO.AiReportContextMessageDTO;
import com.chat.chat.Exceptions.SemanticApiException;
import com.chat.chat.Utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Validated
public class DeepSeekAiReportAnalysisServiceImpl implements AiReportAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiReportAnalysisServiceImpl.class);
    private static final int MAX_MESSAGE_LENGTH = 250;
    private static final int MAX_AUTHOR_LENGTH = 80;
    private static final int MAX_DATE_LENGTH = 40;
    private static final int MAX_REASON_LENGTH = 100;
    private static final int MAX_SUMMARY_LENGTH = 300;
    private static final int MAX_DESCRIPTION_LENGTH = 1500;
    private static final int MAX_ACTION_LENGTH = 80;

    private final AiProperties aiProperties;
    private final DeepSeekProperties deepSeekProperties;
    private final DeepSeekApiClient deepSeekApiClient;
    private final AiRateLimitService aiRateLimitService;
    private final SecurityUtils securityUtils;

    public DeepSeekAiReportAnalysisServiceImpl(AiProperties aiProperties,
                                               DeepSeekProperties deepSeekProperties,
                                               DeepSeekApiClient deepSeekApiClient,
                                               AiRateLimitService aiRateLimitService,
                                               SecurityUtils securityUtils) {
        this.aiProperties = aiProperties;
        this.deepSeekProperties = deepSeekProperties;
        this.deepSeekApiClient = deepSeekApiClient;
        this.aiRateLimitService = aiRateLimitService;
        this.securityUtils = securityUtils;
    }

    @Override
    public AiReportAnalysisResponseDTO analizarDenuncia(AiReportAnalysisRequestDTO request) {
        Long userId = securityUtils.getAuthenticatedUserId();

        if (!aiProperties.isEnabled()) {
            return failure("AI_DISABLED", "La ayuda de IA no esta habilitada.");
        }
        if (!"deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
            return failure("AI_PROVIDER_NOT_SUPPORTED", "El proveedor de IA configurado no es compatible.");
        }
        if (!hasText(deepSeekProperties.getApiKey())) {
            return failure("AI_API_KEY_MISSING", "La API Key de DeepSeek no esta configurada.");
        }
        if (request == null || request.getUsuarioDenunciadoId() == null) {
            return failure("AI_REPORT_INVALID_REQUEST", "usuarioDenunciadoId es obligatorio");
        }

        List<String> motivosDisponibles = sanitizeMotivosDisponibles(request.getMotivosDisponibles());
        if (motivosDisponibles.isEmpty()) {
            return failure("AI_REPORT_INVALID_REASONS", "Debes indicar al menos un motivo disponible para analizar la denuncia.");
        }

        int maxMensajes = resolveMaxMensajes(request.getMaxMensajes());
        List<AiReportContextMessageDTO> sanitizedMessages = sanitizeMessages(request.getMensajes(), maxMensajes);
        if (sanitizedMessages.isEmpty()) {
            return failure("AI_REPORT_EMPTY_CONTEXT", "No hay mensajes suficientes para analizar la denuncia.");
        }

        String userContent = buildConversationContext(request, sanitizedMessages, motivosDisponibles);
        if (!hasText(userContent)) {
            return failure("AI_REPORT_EMPTY_CONTEXT", "No hay mensajes suficientes para analizar la denuncia.");
        }
        if (userContent.length() > aiProperties.getReportAnalysis().getMaxInputLength()) {
            return failure("AI_REPORT_CONTEXT_TOO_LONG", "El contexto enviado es demasiado largo para analizar la denuncia.");
        }

        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            return failure(rateLimitCheck.getCode(), rateLimitCheck.getMessage());
        }

        try {
            LOGGER.info("[AI][REPORT_ANALYSIS] request userId={} usuarioDenunciadoId={} motivos={} mensajes={} contextLength={}",
                    userId,
                    request.getUsuarioDenunciadoId(),
                    motivosDisponibles.size(),
                    sanitizedMessages.size(),
                    userContent.length());
            String rawOutput = deepSeekApiClient.completarTexto(
                    buildSystemPrompt(),
                    userContent,
                    deepSeekProperties.getReportAnalysisMaxOutputTokens()
            );
            aiRateLimitService.registrarUso(userId);
            ReportAnalysisResult result = parseAnalysis(rawOutput, motivosDisponibles);
            if (!result.valid()) {
                return failure(result.code(), result.message());
            }
            return success(result.reason(), result.description(), result.severity(), result.summary(), result.suggestedAction());
        } catch (SemanticApiException ex) {
            LOGGER.warn("[AI][REPORT_ANALYSIS] provider-error userId={} usuarioDenunciadoId={} code={} status={}",
                    userId,
                    request.getUsuarioDenunciadoId(),
                    ex.getCode(),
                    ex.getStatus().value());
            return failure(ex.getCode(), ex.getMessage());
        }
    }

    private List<String> sanitizeMotivosDisponibles(List<String> motivosDisponibles) {
        if (motivosDisponibles == null || motivosDisponibles.isEmpty()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String motivo : motivosDisponibles) {
            String cleaned = cleanupCommon(motivo, MAX_REASON_LENGTH);
            if (!hasText(cleaned)) {
                continue;
            }
            unique.putIfAbsent(normalizeComparable(cleaned), cleaned);
        }
        return new ArrayList<>(unique.values());
    }

    private int resolveMaxMensajes(Integer requestedMaxMensajes) {
        int maxAllowed = aiProperties.getReportAnalysis().getMaxMessages();
        if (requestedMaxMensajes == null || requestedMaxMensajes < 1) {
            return maxAllowed;
        }
        return Math.min(requestedMaxMensajes, maxAllowed);
    }

    private List<AiReportContextMessageDTO> sanitizeMessages(List<AiReportContextMessageDTO> messages, int maxMensajes) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, messages.size() - maxMensajes);
        int maxInputLength = aiProperties.getReportAnalysis().getMaxInputLength();
        int totalLength = 0;
        List<AiReportContextMessageDTO> sanitized = new ArrayList<>();
        for (int i = start; i < messages.size(); i++) {
            AiReportContextMessageDTO message = messages.get(i);
            if (message == null) {
                continue;
            }
            String content = normalizeInput(message.getContenido());
            if (!hasText(content) || isNonTextContent(content)) {
                continue;
            }
            content = truncate(content, MAX_MESSAGE_LENGTH);
            String author = defaultAuthor(message.getAutor(), message.isEsUsuarioActual(), message.isEsUsuarioDenunciado());
            String date = truncate(normalizeInput(message.getFecha()), MAX_DATE_LENGTH);
            String line = buildContextLine(author, date, content, message.isEsUsuarioActual(), message.isEsUsuarioDenunciado());
            if (!hasText(line)) {
                continue;
            }
            if (totalLength + line.length() + 1 > maxInputLength) {
                break;
            }
            AiReportContextMessageDTO current = new AiReportContextMessageDTO();
            current.setId(message.getId());
            current.setAutor(author);
            current.setAutorId(message.getAutorId());
            current.setContenido(content);
            current.setEsUsuarioActual(message.isEsUsuarioActual());
            current.setEsUsuarioDenunciado(message.isEsUsuarioDenunciado());
            current.setFecha(date);
            sanitized.add(current);
            totalLength += line.length() + 1;
        }
        return sanitized;
    }

    private String buildConversationContext(AiReportAnalysisRequestDTO request,
                                            List<AiReportContextMessageDTO> messages,
                                            List<String> motivosDisponibles) {
        StringBuilder builder = new StringBuilder();
        builder.append("Usuario denunciado: ");
        if (hasText(request.getNombreUsuarioDenunciado())) {
            builder.append(truncate(normalizeInput(request.getNombreUsuarioDenunciado()), 120));
        } else {
            builder.append("Usuario ").append(request.getUsuarioDenunciadoId());
        }
        builder.append('\n');
        builder.append("Motivos disponibles: ").append(String.join(", ", motivosDisponibles)).append('\n');
        builder.append("Mensajes recientes:\n");
        for (AiReportContextMessageDTO message : messages) {
            String line = buildContextLine(
                    message.getAutor(),
                    message.getFecha(),
                    message.getContenido(),
                    message.isEsUsuarioActual(),
                    message.isEsUsuarioDenunciado()
            );
            if (!hasText(line)) {
                continue;
            }
            builder.append(line).append('\n');
        }
        builder.append("Reglas:\n");
        builder.append("- No crees denuncias reales.\n");
        builder.append("- No sanciones automaticamente.\n");
        builder.append("- Usa solo un motivo de la lista disponible.\n");
        builder.append("- Si no hay evidencia suficiente, indicalo claramente.");
        return builder.toString();
    }

    private String buildContextLine(String author, String date, String content, boolean currentUser, boolean reportedUser) {
        if (!hasText(content)) {
            return null;
        }
        StringBuilder line = new StringBuilder();
        if (hasText(date)) {
            line.append('[').append(date).append("] ");
        }
        if (reportedUser) {
            line.append("[DENUNCIADO] ");
        } else if (currentUser) {
            line.append("[YO] ");
        } else {
            line.append("[OTRO] ");
        }
        line.append(author).append(": ").append(content);
        return line.toString();
    }

    private String buildSystemPrompt() {
        return "Analiza los ultimos mensajes de este chat para ayudar a rellenar una denuncia de usuario. "
                + "Debes elegir el motivo mas adecuado usando exclusivamente la lista de motivos disponibles. "
                + "No inventes motivos nuevos. Si el usuario denunciado ha dicho algo relevante, resume que ha dicho o que comportamiento aparece en los mensajes. "
                + "Redacta una descripcion clara para el formulario de denuncia. No exageres, no inventes hechos y no acuses de cosas que no aparezcan en el contexto. "
                + "Si no hay evidencia suficiente, indicalo. "
                + "Devuelve exactamente este formato:\n"
                + "MOTIVO: <uno de los motivos disponibles>\n"
                + "GRAVEDAD: <BAJA|MEDIA|ALTA>\n"
                + "RESUMEN: <resumen breve>\n"
                + "DESCRIPCION: <texto de denuncia listo para rellenar el formulario>\n"
                + "ACCION_SUGERIDA: <advertencia|revision manual|sin accion|bloqueo temporal si procede>";
    }

    private ReportAnalysisResult parseAnalysis(String rawOutput, List<String> motivosDisponibles) {
        if (!hasText(rawOutput)) {
            return ReportAnalysisResult.failure("AI_REPORT_PARSE_ERROR", "No se pudo generar un analisis valido de denuncia.");
        }
        String normalized = rawOutput.replace("\r\n", "\n").replace('\r', '\n');
        Map<String, String> fields = new LinkedHashMap<>();
        String currentField = null;
        StringBuilder currentValue = new StringBuilder();

        for (String rawLine : normalized.split("\n")) {
            String line = rawLine == null ? null : rawLine.trim();
            if (!hasText(line)) {
                continue;
            }
            int separator = line.indexOf(':');
            if (separator > 0) {
                String candidateField = normalizeComparable(line.substring(0, separator));
                if (isSupportedField(candidateField)) {
                    if (currentField != null) {
                        fields.put(currentField, normalizeInput(currentValue.toString()));
                    }
                    currentField = candidateField;
                    currentValue = new StringBuilder(line.substring(separator + 1).trim());
                    continue;
                }
            }
            if (currentField != null) {
                if (currentValue.length() > 0) {
                    currentValue.append(' ');
                }
                currentValue.append(line);
            }
        }
        if (currentField != null) {
            fields.put(currentField, normalizeInput(currentValue.toString()));
        }

        String motivo = resolveMotivo(fields.get("motivo"), motivosDisponibles);
        if (!hasText(motivo)) {
            return ReportAnalysisResult.failure("AI_REPORT_NO_CLEAR_REASON", "No se ha detectado un motivo claro de denuncia en los mensajes recientes.");
        }

        String resumen = cleanupCommon(fields.get("resumen"), MAX_SUMMARY_LENGTH);
        String descripcion = cleanupCommon(fields.get("descripcion"), MAX_DESCRIPTION_LENGTH);
        if (!hasText(descripcion)) {
            descripcion = resumen;
        }
        if (!hasText(resumen) || !hasText(descripcion)) {
            return ReportAnalysisResult.failure("AI_REPORT_PARSE_ERROR", "No se pudo generar un analisis valido de denuncia.");
        }

        return ReportAnalysisResult.success(
                motivo,
                descripcion,
                normalizeSeverity(fields.get("gravedad")),
                resumen,
                cleanupAction(fields.get("accionsugerida"))
        );
    }

    private boolean isSupportedField(String field) {
        return "motivo".equals(field)
                || "gravedad".equals(field)
                || "resumen".equals(field)
                || "descripcion".equals(field)
                || "accionsugerida".equals(field);
    }

    private String resolveMotivo(String rawMotivo, List<String> motivosDisponibles) {
        String cleaned = cleanupCommon(rawMotivo, MAX_REASON_LENGTH);
        if (hasText(cleaned)) {
            for (String motivo : motivosDisponibles) {
                if (motivo.equals(cleaned)) {
                    return motivo;
                }
            }

            String normalized = normalizeComparable(cleaned);
            for (String motivo : motivosDisponibles) {
                if (normalizeComparable(motivo).equals(normalized)) {
                    return motivo;
                }
            }

            String closest = findClosestMotivo(normalized, motivosDisponibles);
            if (hasText(closest)) {
                return closest;
            }
        }
        return findGenericMotivo(motivosDisponibles);
    }

    private String findClosestMotivo(String normalizedMotivo, List<String> motivosDisponibles) {
        if (!hasText(normalizedMotivo)) {
            return null;
        }
        for (String motivo : motivosDisponibles) {
            String current = normalizeComparable(motivo);
            if (current.contains(normalizedMotivo) || normalizedMotivo.contains(current)) {
                return motivo;
            }
        }
        return null;
    }

    private String findGenericMotivo(List<String> motivosDisponibles) {
        for (String motivo : motivosDisponibles) {
            String normalized = normalizeComparable(motivo);
            if ("otro".equals(normalized) || "otros".equals(normalized) || normalized.startsWith("otro")) {
                return motivo;
            }
        }
        return null;
    }

    private String normalizeSeverity(String rawSeverity) {
        String normalized = normalizeComparable(rawSeverity);
        return switch (normalized) {
            case "baja" -> "BAJA";
            case "alta" -> "ALTA";
            default -> "MEDIA";
        };
    }

    private String cleanupAction(String value) {
        return cleanupCommon(value, MAX_ACTION_LENGTH);
    }

    private String cleanupCommon(String value, int maxLength) {
        String normalized = normalizeInput(value);
        if (!hasText(normalized)) {
            return null;
        }
        normalized = normalized.replaceAll("^['\"`\\-:;,.\\s]+", "");
        normalized = normalized.replaceAll("['\"`\\s]+$", "");
        normalized = normalized.replaceAll("\\s{2,}", " ").trim();
        if (!hasText(normalized)) {
            return null;
        }
        return truncate(normalized, maxLength);
    }

    private boolean isNonTextContent(String content) {
        String normalized = content.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("data:image")
                || normalized.startsWith("data:audio")
                || normalized.startsWith("data:video")
                || normalized.startsWith("data:application")
                || normalized.equals("[audio]")
                || normalized.equals("[imagen]")
                || normalized.equals("[image]")
                || normalized.equals("[video]")
                || normalized.equals("[file]")
                || normalized.equals("[archivo]")
                || normalized.equals("[sticker]");
    }

    private String defaultAuthor(String author, boolean currentUser, boolean reportedUser) {
        String normalized = normalizeInput(author);
        if (hasText(normalized)) {
            return truncate(normalized, MAX_AUTHOR_LENGTH);
        }
        if (reportedUser) {
            return "Usuario denunciado";
        }
        return currentUser ? "Usuario actual" : "Participante";
    }

    private String normalizeInput(String text) {
        if (text == null) {
            return null;
        }
        String compact = text.replace("\r\n", "\n").replace('\r', '\n');
        compact = compact.replaceAll("[\\t\\f\\x0B]+", " ");
        compact = compact.replaceAll(" {2,}", " ");
        compact = compact.replaceAll("\\n{2,}", " ");
        return compact.trim();
    }

    private String normalizeComparable(String text) {
        String normalized = normalizeInput(text);
        if (!hasText(normalized)) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private AiReportAnalysisResponseDTO success(String motivoSeleccionado,
                                                String descripcionDenuncia,
                                                String gravedad,
                                                String resumen,
                                                String accionSugerida) {
        AiReportAnalysisResponseDTO response = new AiReportAnalysisResponseDTO();
        response.setSuccess(true);
        response.setCodigo("OK");
        response.setMensaje("Analisis de denuncia generado correctamente");
        response.setMotivoSeleccionado(motivoSeleccionado);
        response.setDescripcionDenuncia(descripcionDenuncia);
        response.setGravedad(gravedad);
        response.setResumen(resumen);
        response.setAccionSugerida(accionSugerida);
        return response;
    }

    private AiReportAnalysisResponseDTO failure(String code, String message) {
        AiReportAnalysisResponseDTO response = new AiReportAnalysisResponseDTO();
        response.setSuccess(false);
        response.setCodigo(code);
        response.setMensaje(message);
        response.setMotivoSeleccionado(null);
        response.setDescripcionDenuncia(null);
        response.setGravedad(null);
        response.setResumen(null);
        response.setAccionSugerida(null);
        return response;
    }

    private record ReportAnalysisResult(boolean valid,
                                        String code,
                                        String message,
                                        String reason,
                                        String description,
                                        String severity,
                                        String summary,
                                        String suggestedAction) {

        private static ReportAnalysisResult success(String reason,
                                                    String description,
                                                    String severity,
                                                    String summary,
                                                    String suggestedAction) {
            return new ReportAnalysisResult(true, "OK", null, reason, description, severity, summary, suggestedAction);
        }

        private static ReportAnalysisResult failure(String code, String message) {
            return new ReportAnalysisResult(false, code, message, null, null, null, null, null);
        }
    }
}
