package com.chat.chat.Service.EmailService;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendHtmlEmail(String to, String subject, String templatePath, Map<String, String> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);

            // 1. Cargar la plantilla desde resources
            ClassPathResource resource = new ClassPathResource(templatePath);
            String htmlContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            // 2. Reemplazar variables dinámicamente: {{variable}} -> valor
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                htmlContent = htmlContent.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            helper.setText(htmlContent, true);
            mailSender.send(message);
            
        } catch (Exception e) {
            System.err.println("❌ Error enviando email a " + to + ": " + e.getMessage());
            // No lanzamos excepción para no interrumpir el flujo principal (Baneo en DB)
        }
    }
}
