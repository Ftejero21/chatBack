package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiEncryptedContextMessageDTO;
import com.chat.chat.DTO.AiEncryptedResponseDTO;
import com.chat.chat.DTO.E2EMessagePayloadDTO;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Service.MensajeProgramadoService.ExcepcionCifradoProgramado;
import com.chat.chat.Utils.AdminAuditCrypto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;

@Service
public class AiEncryptedContextServiceImpl implements AiEncryptedContextService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiEncryptedContextServiceImpl.class);
    private static final String TRANSFORM_AES_GCM = "AES/GCM/NoPadding";
    private static final String TRANSFORM_RSA_OAEP_SHA256 = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final OAEPParameterSpec OAEP_SHA256_SPEC = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int AES_KEY_SIZE_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final AdminAuditCrypto adminAuditCrypto;
    private final UsuarioRepository usuarioRepository;

    public AiEncryptedContextServiceImpl(AdminAuditCrypto adminAuditCrypto,
                                         UsuarioRepository usuarioRepository) {
        this.adminAuditCrypto = adminAuditCrypto;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<AiPlainContextMessage> decryptContextMessages(List<AiEncryptedContextMessageDTO> mensajes) {
        if (mensajes == null || mensajes.isEmpty()) {
            return List.of();
        }
        List<AiPlainContextMessage> result = new ArrayList<>();
        for (AiEncryptedContextMessageDTO mensaje : mensajes) {
            if (mensaje == null) {
                continue;
            }
            String contenido = decryptMessagePayload(mensaje.getEncryptedPayload());
            if (contenido == null || contenido.isBlank()) {
                LOGGER.warn("[AI][SUMMARY_ENCRYPTED] skip-undecipherable-message id={}", mensaje.getId());
                continue;
            }
            AiPlainContextMessage current = new AiPlainContextMessage();
            current.setId(mensaje.getId());
            current.setAutorId(mensaje.getAutorId());
            current.setAutor(mensaje.getAutor());
            current.setFecha(mensaje.getFecha());
            current.setEsUsuarioActual(mensaje.isEsUsuarioActual());
            current.setContenido(contenido);
            result.add(current);
        }
        return result;
    }

    @Override
    public String decryptMessagePayload(String encryptedPayload) {
        if (encryptedPayload == null || encryptedPayload.isBlank()) {
            return null;
        }
        try {
            E2EMessagePayloadDTO payload = objectMapper.readValue(encryptedPayload, E2EMessagePayloadDTO.class);
            if (payload == null || isBlank(payload.getForAdmin()) || isBlank(payload.getIv()) || isBlank(payload.getCiphertext())) {
                return null;
            }

            byte[] adminEnvelopeBytes = adminAuditCrypto.decryptBase64EnvelopeBytes(payload.getForAdmin());
            if (adminEnvelopeBytes == null || adminEnvelopeBytes.length == 0) {
                return null;
            }

            String decrypted = decryptUsingAdminEnvelope(payload, adminEnvelopeBytes);
            if (!isBlank(decrypted)) {
                return decrypted;
            }

            // Compatibilidad con payloads antiguos donde forAdmin contenia el texto directamente.
            return safeUtf8(adminEnvelopeBytes);
        } catch (Exception ex) {
            return null;
        }
    }

    private String decryptUsingAdminEnvelope(E2EMessagePayloadDTO payload, byte[] adminEnvelopeBytes) {
        byte[] aesKey = resolveAesKey(adminEnvelopeBytes);
        if (aesKey == null) {
            return null;
        }
        try {
            byte[] iv = Base64.getDecoder().decode(payload.getIv());
            byte[] ciphertext = Base64.getDecoder().decode(payload.getCiphertext());
            Cipher cipher = Cipher.getInstance(TRANSFORM_AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] resolveAesKey(byte[] adminEnvelopeBytes) {
        if (isValidAesKeyLength(adminEnvelopeBytes.length)) {
            return adminEnvelopeBytes;
        }
        String asUtf8 = safeUtf8(adminEnvelopeBytes);
        if (isBlank(asUtf8)) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(asUtf8);
            return isValidAesKeyLength(decoded.length) ? decoded : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isValidAesKeyLength(int len) {
        return len == 16 || len == 24 || len == 32;
    }

    private String safeUtf8(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public AiEncryptedResponseDTO encryptAiResponseForUser(String textoPlano, Long userId) {
        UsuarioEntity user = usuarioRepository.findById(userId).orElse(null);
        if (user == null || user.getPublicKey() == null || user.getPublicKey().isBlank()) {
            return failure("AI_USER_PUBLIC_KEY_MISSING", "No se ha encontrado una clave publica valida para devolver el resumen cifrado.");
        }
        try {
            String payload = encryptForUserWithoutAdminAudit(textoPlano, user);
            AiEncryptedResponseDTO response = new AiEncryptedResponseDTO();
            response.setSuccess(true);
            response.setCodigo("OK");
            response.setMensaje("Conversacion resumida correctamente");
            response.setEncryptedPayload(payload);
            return response;
        } catch (ExcepcionCifradoProgramado ex) {
            LOGGER.warn("[AI][SUMMARY_ENCRYPTED] response-encryption-user-key-error userId={} recuperable={} errorClass={}",
                    userId,
                    ex.isRecuperable(),
                    ex.getClass().getSimpleName());
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (message.contains("clave publica ausente")) {
                return failure("AI_USER_PUBLIC_KEY_MISSING", "El usuario actual no tiene una clave publica E2E configurada.");
            }
            if (message.contains("clave publica invalida")) {
                return failure("AI_USER_PUBLIC_KEY_INVALID", "La clave publica E2E del usuario actual no tiene un formato valido.");
            }
            return failure("AI_RESPONSE_ENCRYPTION_ERROR", "No se pudo cifrar el resumen para el usuario actual.");
        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][SUMMARY_ENCRYPTED] response-encryption-failed userId={} errorClass={}",
                    userId,
                    ex.getClass().getSimpleName());
            return failure("AI_RESPONSE_ENCRYPTION_ERROR", "No se pudo cifrar el resumen para el usuario actual.");
        }
    }

    private AiEncryptedResponseDTO failure(String code, String message) {
        AiEncryptedResponseDTO response = new AiEncryptedResponseDTO();
        response.setSuccess(false);
        response.setCodigo(code);
        response.setMensaje(message);
        response.setEncryptedPayload(null);
        return response;
    }

    private String encryptForUserWithoutAdminAudit(String textoPlano, UsuarioEntity user) {
        PublicKey publicKey = parseUserPublicKey(user);
        MaterialCifrado material = encryptPlainTextWithAes(textoPlano);
        String envelope = encryptAesKey(material.claveAes(), publicKey);

        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "E2E");
        payload.put("iv", material.ivBase64());
        payload.put("ciphertext", material.ciphertextBase64());
        payload.put("forEmisor", envelope);
        payload.put("forReceptor", envelope);
        return writeJson(payload);
    }

    private PublicKey parseUserPublicKey(UsuarioEntity user) {
        try {
            String normalized = user.getPublicKey()
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(normalized);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new ExcepcionCifradoProgramado("clave publica invalida para receptor", ex, true);
        }
    }

    private MaterialCifrado encryptPlainTextWithAes(String text) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(AES_KEY_SIZE_BITS);
            SecretKey secretKey = keyGenerator.generateKey();

            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM_AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return new MaterialCifrado(secretKey.getEncoded(), Base64.getEncoder().encodeToString(iv), Base64.getEncoder().encodeToString(ciphertext));
        } catch (Exception ex) {
            throw new ExcepcionCifradoProgramado("fallo cifrado AES-GCM para respuesta IA", ex, false);
        }
    }

    private String encryptAesKey(byte[] aesKey, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORM_RSA_OAEP_SHA256);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA256_SPEC);
            return Base64.getEncoder().encodeToString(cipher.doFinal(aesKey));
        } catch (Exception ex) {
            throw new ExcepcionCifradoProgramado("fallo cifrado RSA-OAEP en respuesta IA", ex, true);
        }
    }

    private String writeJson(LinkedHashMap<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ExcepcionCifradoProgramado("fallo serializando payload E2E de respuesta IA", ex, false);
        }
    }

    private record MaterialCifrado(byte[] claveAes, String ivBase64, String ciphertextBase64) {
    }
}
