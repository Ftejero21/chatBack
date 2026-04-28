package com.chat.chat.Utils;

import java.util.Locale;

public enum AiTextMode {
    AUTO,
    CORREGIR,
    REFORMULAR,
    FORMAL,
    INFORMAL,
    RESUMIR,
    RESPONDER,
    EXPLICAR,
    GENERAR_EMAIL,
    GENERAR_RESPUESTA,
    COMPLETAR_TEXTO;

    public static AiTextMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (AiTextMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Modo de IA invalido");
    }
}
