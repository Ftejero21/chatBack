package com.chat.chat.Service.AdminReportPdfService;

import com.chat.chat.Exceptions.SemanticApiException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class AdminReportPdfServiceImpl implements AdminReportPdfService {

    @Override
    public byte[] convertHtmlToPdf(String html) {
        if (html == null || html.isBlank()) {
            throw new SemanticApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "ADMIN_AI_REPORT_ERROR",
                    "No se pudo generar el HTML interno del reporte.",
                    null);
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            byte[] pdfBytes = outputStream.toByteArray();
            if (pdfBytes.length < 5
                    || pdfBytes[0] != '%'
                    || pdfBytes[1] != 'P'
                    || pdfBytes[2] != 'D'
                    || pdfBytes[3] != 'F') {
                throw new SemanticApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "ADMIN_AI_REPORT_ERROR",
                        "La conversion del reporte a PDF no produjo un archivo valido.",
                        null);
            }
            return pdfBytes;
        } catch (SemanticApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SemanticApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "ADMIN_AI_REPORT_ERROR",
                    "No se pudo convertir el reporte a PDF.",
                    null);
        }
    }
}
