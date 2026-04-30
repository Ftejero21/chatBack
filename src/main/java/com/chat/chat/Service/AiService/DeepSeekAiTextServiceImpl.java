package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.Configuracion.DeepSeekProperties;
import com.chat.chat.DTO.AiTextRequestDTO;
import com.chat.chat.DTO.AiTextResponseDTO;
import com.chat.chat.Exceptions.SemanticApiException;
import com.chat.chat.Utils.AiTextMode;
import com.chat.chat.Utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Validated
public class DeepSeekAiTextServiceImpl implements AiTextService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiTextServiceImpl.class);
    private static final Pattern COMPLETE_MARKER_PATTERN = Pattern.compile("(?i)\\[?COMPLETAR CON IA\\]?");

    private final AiProperties aiProperties;
    private final DeepSeekProperties deepSeekProperties;
    private final DeepSeekApiClient deepSeekApiClient;
    private final AiRateLimitService aiRateLimitService;
    private final SecurityUtils securityUtils;

    public DeepSeekAiTextServiceImpl(AiProperties aiProperties,
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
    public AiTextResponseDTO procesarTexto(AiTextRequestDTO request) {
        String originalText = request == null ? null : request.getTexto();
        String targetLanguage = request == null ? null : request.getIdiomaDestino();
        Long userId = securityUtils.getAuthenticatedUserId();

        if (!aiProperties.isEnabled()) {
            return failure(originalText, null, "AI_DISABLED", "La ayuda de IA no esta habilitada.");
        }
        if (!"deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
            return failure(originalText, null, "AI_PROVIDER_NOT_SUPPORTED", "El proveedor de IA configurado no es compatible.");
        }
        if (!hasText(originalText)) {
            return failure(originalText, null, "AI_TEXT_EMPTY", "El texto es obligatorio.");
        }

        String normalizedOriginal = normalizeInput(originalText);
        if (!hasText(normalizedOriginal)) {
            return failure(originalText, null, "AI_TEXT_EMPTY", "El texto es obligatorio.");
        }

        AiTextMode requestedMode;
        try {
            requestedMode = AiTextMode.fromValue(request.getModo());
        } catch (IllegalArgumentException ex) {
            return failure(originalText, null, "AI_MODE_INVALID", "El modo de IA no es valido.");
        }

        AiTextMode effectiveMode = resolveMode(requestedMode, normalizedOriginal);
        String normalizedTargetLanguage = normalizeInput(targetLanguage);
        if (effectiveMode == AiTextMode.TRADUCIR && !hasText(normalizedTargetLanguage)) {
            return failure(originalText, effectiveMode.name(), "AI_TARGET_LANGUAGE_REQUIRED", "Debes seleccionar un idioma de destino para traducir.");
        }
        int maxLength = resolveMaxInputLength(effectiveMode);
        logLengthValidation(effectiveMode, normalizedOriginal.length(), maxLength);
        if (normalizedOriginal.length() > maxLength) {
            return failure(originalText, effectiveMode.name(), "AI_TEXT_TOO_LONG", buildLengthExceededMessage(effectiveMode, normalizedOriginal.length(), maxLength));
        }
        if (!hasText(deepSeekProperties.getApiKey())) {
            return failure(originalText, null, "AI_API_KEY_MISSING", "La API Key de DeepSeek no esta configurada.");
        }

        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            return failure(originalText, null, rateLimitCheck.getCode(), rateLimitCheck.getMessage());
        }

        String cleanedUserText = cleanupForMode(normalizedOriginal);
        if (!hasText(cleanedUserText)) {
            return failure(originalText, effectiveMode.name(), "AI_TEXT_EMPTY", "El texto es obligatorio.");
        }
        logLengthValidation(effectiveMode, cleanedUserText.length(), maxLength);
        if (cleanedUserText.length() > maxLength) {
            return failure(originalText, effectiveMode.name(), "AI_TEXT_TOO_LONG", buildLengthExceededMessage(effectiveMode, cleanedUserText.length(), maxLength));
        }

        try {
            LOGGER.info("[AI][TEXT] request userId={} mode={} textLength={}",
                    userId,
                    effectiveMode.name(),
                    cleanedUserText.length());
            String generatedText = deepSeekApiClient.completarTexto(
                    buildPrompt(effectiveMode),
                    buildUserContent(effectiveMode, cleanedUserText, normalizedTargetLanguage)
            );
            aiRateLimitService.registrarUso(userId);
            return success(originalText, generatedText, effectiveMode.name());
        } catch (SemanticApiException ex) {
            LOGGER.warn("[AI][TEXT] provider-error userId={} mode={} code={} status={}",
                    userId,
                    effectiveMode.name(),
                    ex.getCode(),
                    ex.getStatus().value());
            return failure(originalText, effectiveMode.name(), ex.getCode(), ex.getMessage());
        }
    }

    private AiTextMode resolveMode(AiTextMode requestedMode, String text) {
        if (requestedMode != null && requestedMode != AiTextMode.AUTO) {
            return requestedMode;
        }
        String normalized = text == null ? "" : text.trim();
        if (containsCompleteMarker(normalized) || normalized.endsWith(":")) {
            return AiTextMode.COMPLETAR_TEXTO;
        }
        return AiTextMode.REFORMULAR;
    }

    private boolean containsCompleteMarker(String text) {
        return text != null && COMPLETE_MARKER_PATTERN.matcher(text).find();
    }

    private String cleanupForMode(String text) {
        if (text == null) {
            return null;
        }
        String withoutMarker = COMPLETE_MARKER_PATTERN.matcher(text).replaceAll(" ");
        return normalizeInput(withoutMarker);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeInput(String text) {
        if (text == null) {
            return null;
        }
        String compact = text.replace("\r\n", "\n").replace('\r', '\n');
        compact = compact.replaceAll("[\\t\\f\\x0B]+", " ");
        compact = compact.replaceAll(" {2,}", " ");
        compact = compact.replaceAll("\\n{3,}", "\n\n");
        return compact.trim();
    }

    private String buildPrompt(AiTextMode mode) {
        return switch (mode) {
            case CORREGIR -> "Corrige la ortografia, gramatica y puntuacion del siguiente mensaje. Manten exactamente el significado original. No anadas informacion nueva. No expliques los cambios. Devuelve unicamente el texto corregido.";
            case REFORMULAR -> "Reformula el siguiente mensaje para que sea mas claro, natural y bien escrito. Manten el significado original. No anadas informacion nueva. No expliques los cambios. Devuelve unicamente el mensaje reformulado.";
            case FORMAL -> "Convierte el siguiente mensaje en un texto mas formal, respetuoso y profesional. Manten el significado original. No anadas informacion nueva. Devuelve unicamente el mensaje final.";
            case INFORMAL -> "Convierte el siguiente mensaje en un texto mas informal, natural y cercano. Manten el significado original. No anadas informacion nueva. Devuelve unicamente el mensaje final.";
            case RESUMIR -> "Resume el siguiente texto de forma breve y clara. No anadas informacion nueva. Devuelve unicamente el resumen.";
            case RESPONDER -> "Genera una posible respuesta natural para el siguiente contexto o mensaje recibido. No inventes datos importantes. Si falta contexto, responde de forma prudente. Devuelve unicamente la respuesta.";
            case EXPLICAR -> "Explica el siguiente texto de forma sencilla y clara. No anadas informacion no relacionada. Devuelve unicamente la explicacion.";
            case TRADUCIR -> "Traduce el siguiente mensaje al idioma indicado por el usuario. Manten el significado original, el tono, la intencion, la formalidad y los emojis si encajan. Si el mensaje es informal, la traduccion debe sonar informal. Si el mensaje es formal, la traduccion debe sonar formal. No anadas informacion nueva. No expliques la traduccion. Devuelve unicamente el texto traducido.";
            case GENERAR_EMAIL -> "Redacta un email formal y completo a partir de la idea del usuario. Devuelve exactamente este formato:\n\nASUNTO: <asunto claro, profesional y breve>\n\nCUERPO:\nBuenas tardes,\n\n<párrafo 1>\n\n<párrafo 2>\n\n<párrafo 3 opcional si hace falta>\n\nMuchas gracias de antemano.\n\nUn saludo,\nTejeChat\n\nReglas:\n- Usa tono profesional, claro y correcto.\n- Usa parrafos separados y saltos de linea.\n- El asunto debe estar bien redactado.\n- El cuerpo no debe ir en una sola linea.\n- No firmes como Fernando.\n- Firma siempre como TejeChat.\n- No anadas explicaciones fuera del email.\n- No inventes datos importantes.";
            case GENERAR_RESPUESTA -> "El usuario quiere generar un mensaje a partir de una explicacion. Convierte la explicacion en un mensaje natural, claro y bien escrito. No inventes datos importantes. No anadas informacion que no este en el texto original. Devuelve unicamente el mensaje final.";
            case COMPLETAR_TEXTO -> "Completa el siguiente texto de forma natural y coherente, respetando el tono original del usuario. Si el texto es informal, responde informal; si es formal, responde formal. Si el texto termina en ':' completa lo que vendria despues. Si el texto queda cortado a mitad de frase, continua la frase de forma natural. Devuelve unicamente el texto final completo, sin explicaciones. No anadas detalles graves, medicos, legales, economicos o sensibles si no aparecen en el texto original. No inventes noticias, resultados deportivos, fichajes, lesiones ni datos actuales como si fueran reales.";
            case AUTO -> throw new IllegalStateException("AUTO no debe llegar al prompt");
        };
    }

    private String buildUserContent(AiTextMode mode, String cleanedUserText, String targetLanguage) {
        if (mode == AiTextMode.TRADUCIR) {
            return "Idioma destino: " + targetLanguage + "\n"
                    + "Texto original: " + cleanedUserText;
        }
        return cleanedUserText;
    }

    private int resolveMaxInputLength(AiTextMode mode) {
        return mode == AiTextMode.RESPONDER
                ? aiProperties.getMaxInputLengthResponder()
                : aiProperties.getMaxInputLength();
    }

    private void logLengthValidation(AiTextMode mode, int textLength, int maxLength) {
        LOGGER.info("[AI][TEXT] length-check modoFinal={} longitudTexto={} limiteAplicado={}",
                mode.name(),
                textLength,
                maxLength);
    }

    private String buildLengthExceededMessage(AiTextMode mode, int textLength, int maxLength) {
        return "El texto supera la longitud maxima permitida para " + mode.name() + ": " + textLength + "/" + maxLength;
    }

    private AiTextResponseDTO success(String originalText, String generatedText, String mode) {
        AiTextResponseDTO response = new AiTextResponseDTO();
        response.setTextoOriginal(originalText);
        response.setTextoGenerado(generatedText);
        response.setModo(mode);
        response.setSuccess(true);
        response.setCodigo("OK");
        response.setMensaje(AiTextMode.TRADUCIR.name().equalsIgnoreCase(mode)
                ? "Texto traducido correctamente"
                : "Texto procesado correctamente");
        return response;
    }

    private AiTextResponseDTO failure(String originalText, String mode, String code, String message) {
        AiTextResponseDTO response = new AiTextResponseDTO();
        response.setTextoOriginal(originalText);
        response.setTextoGenerado(null);
        response.setModo(mode == null ? AiTextMode.AUTO.name() : mode.toUpperCase(Locale.ROOT));
        response.setSuccess(false);
        response.setCodigo(code);
        response.setMensaje(message);
        return response;
    }
}
