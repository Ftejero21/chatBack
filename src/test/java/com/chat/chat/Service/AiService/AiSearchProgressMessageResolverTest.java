package com.chat.chat.Service.AiService;

import com.chat.chat.Utils.AiSearchProgressStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiSearchProgressMessageResolverTest {

    private final AiSearchProgressMessageResolver resolver = new AiSearchProgressMessageResolver();

    @Test
    void imageFoundMessage() {
        String message = resolver.resolve(AiSearchProgressStep.MESSAGE_FOUND, "COMPLETED", "MESSAGES", "IMAGE", null);
        assertEquals("Imagen encontrada", message);
    }

    @Test
    void audioAnalyzingMessagesStarted() {
        String message = resolver.resolve(AiSearchProgressStep.ANALYZING_MESSAGES, "STARTED", "MESSAGES", "AUDIO", null);
        assertEquals("Buscando audios...", message);
    }

    @Test
    void stickerNotFoundMessage() {
        String message = resolver.resolve(AiSearchProgressStep.MESSAGE_NOT_FOUND, "COMPLETED", "MESSAGES", "STICKER", null);
        assertEquals("No se encontraron stickers", message);
    }

    @Test
    void complaintsReceivedSearchStarted() {
        String message = resolver.resolve(AiSearchProgressStep.COMPLAINTS_SEARCH, "STARTED", "COMPLAINTS_RECEIVED", null, "RECEIVED");
        assertEquals("Buscando denuncias recibidas...", message);
    }

    @Test
    void complaintsCreatedSearchStarted() {
        String message = resolver.resolve(AiSearchProgressStep.COMPLAINTS_SEARCH, "STARTED", "COMPLAINTS_CREATED", null, "CREATED");
        assertEquals("Buscando denuncias realizadas...", message);
    }

    @Test
    void anyKeepsGenericAnalyzingMessages() {
        String message = resolver.resolve(AiSearchProgressStep.ANALYZING_MESSAGES, "STARTED", "MESSAGES", "ANY", null);
        assertEquals("Analizando mensajes...", message);
    }
}
