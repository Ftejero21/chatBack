package com.chat.chat.Service.EmailService;

import com.chat.chat.DTO.EmailAttachmentDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface EmailService {
    /**
     * @param to Destinatario
     * @param subject Asunto del correo
     * @param templatePath Ruta en resources (ej: "templates/email-ban.html")
     * @param variables Mapa con los datos a reemplazar en el HTML {{nombre}}, etc.
     */
    public void sendHtmlEmail(String to, String subject, String templatePath, Map<String, String> variables);

    /**
     * Variante estricta: propaga excepciones si no se puede enviar.
     */
    public void sendHtmlEmailOrThrow(String to, String subject, String templatePath, Map<String, String> variables);

    public void sendHtmlEmailOrThrow(String to,
                                     String subject,
                                     String templatePath,
                                     Map<String, String> variables,
                                     List<MultipartFile> attachments);

    public void sendHtmlEmailWithAttachmentsOrThrow(String to,
                                                    String subject,
                                                    String templatePath,
                                                    Map<String, String> variables,
                                                    List<EmailAttachmentDTO> attachments);
}
