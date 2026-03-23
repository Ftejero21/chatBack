package com.chat.chat.Service.EmailService;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendHtmlEmail(String to, String subject, String templatePath, Map<String, String> variables) {
        LOGGER.info("[EMAIL] sendHtmlEmail to={} subject={} template={}", to, subject, templatePath);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);

            // 1. Cargar la plantilla desde resources
            ClassPathResource resource = new ClassPathResource(templatePath);
            String htmlContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            // 2. Reemplazar variables dinamicamente: {{variable}} -> valor
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                htmlContent = htmlContent.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            helper.setText(htmlContent, true);
            mailSender.send(message);
            LOGGER.info("[EMAIL] enviado to={} subject={}", to, subject);
        } catch (Exception e) {
            LOGGER.error("[EMAIL] error enviando a {}: {}", to, e.getMessage());
            // No lanzamos excepcion para no interrumpir el flujo principal (Baneo en DB)
        }
    }
}
