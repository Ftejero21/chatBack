package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiUiCustomizationIntentInternalRequestDTO;
import com.chat.chat.DTO.AiUiCustomizationIntentInternalResponseDTO;
import com.chat.chat.DTO.AiUiCustomizationRequestDTO;
import com.chat.chat.DTO.AiUiCustomizationResponseDTO;
import com.chat.chat.DTO.UiCustomizationContextDTO;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AiUiCustomizationServiceImpl implements AiUiCustomizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiUiCustomizationServiceImpl.class);

    private final AiUiCustomizationMicroserviceClient client;
    private final AiUiCustomizationValidationService validationService;
    private final SecurityUtils securityUtils;
    private final UsuarioRepository usuarioRepository;

    public AiUiCustomizationServiceImpl(AiUiCustomizationMicroserviceClient client,
                                        AiUiCustomizationValidationService validationService,
                                        SecurityUtils securityUtils,
                                        UsuarioRepository usuarioRepository) {
        this.client = client;
        this.validationService = validationService;
        this.securityUtils = securityUtils;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AiUiCustomizationResponseDTO classifyIntent(AiUiCustomizationRequestDTO request) {
        String requestId = UUID.randomUUID().toString();
        if (request == null || !hasText(request.getConsulta())) {
            return failure(requestId, "UI_CUSTOMIZATION_INVALID_REQUEST", "consulta es obligatoria");
        }
        Long userId = securityUtils.getAuthenticatedUserId();
        if (userId == null) {
            return failure(requestId, "UI_CUSTOMIZATION_UNAUTHORIZED", "Usuario no autenticado");
        }

        AiUiCustomizationIntentInternalRequestDTO internalRequest = new AiUiCustomizationIntentInternalRequestDTO();
        internalRequest.setRequestId(requestId);
        internalRequest.setConsulta(request.getConsulta());
        internalRequest.setUsuarioActualNombre(resolveUserDisplayName(userId));
        internalRequest.setUiContext(request.getUiContext());

        AiUiCustomizationIntentInternalResponseDTO intent = client.classifyIntent(requestId, internalRequest);
        if (intent == null || !intent.isSuccess()) {
            String codigo = intent == null ? "UI_CUSTOMIZATION_SERVICE_UNAVAILABLE" : intent.getCodigo();
            String mensaje = intent == null ? "El servicio IA no esta disponible." : intent.getMensaje();
            return failure(requestId,
                    codigo == null ? "UI_CUSTOMIZATION_SERVICE_ERROR" : codigo,
                    mensaje == null ? "Error IA" : mensaje);
        }

        return validationService.validate(
                requestId,
                request.getConsulta(),
                intent.getAction(),
                intent.getArea(),
                intent.getProperty(),
                intent.getValue(),
                intent.getValuePreset(),
                intent.getLabel(),
                intent.getConfidence(),
                intent.getColorIntent(),
                intent.getScope(),
                intent.getNeedsClarification(),
                intent.getClarificationReason(),
                intent.getClarificationQuestion(),
                intent.getChanges(),
                request.getUiContext()
        );
    }

    private AiUiCustomizationResponseDTO failure(String requestId, String codigo, String mensaje) {
        LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false codigo={}", requestId, codigo);
        AiUiCustomizationResponseDTO response = new AiUiCustomizationResponseDTO();
        response.setSuccess(false);
        response.setCodigo(codigo);
        response.setMensaje(mensaje);
        response.setTarget("UI_CUSTOMIZATION");
        return response;
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return usuarioRepository.findById(userId)
                    .map(user -> {
                        String nombre = (user.getNombre() == null ? "" : user.getNombre()).trim();
                        String apellido = (user.getApellido() == null ? "" : user.getApellido()).trim();
                        String fullName = (nombre + " " + apellido).trim();
                        return fullName.isEmpty() ? user.getEmail() : fullName;
                    })
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
