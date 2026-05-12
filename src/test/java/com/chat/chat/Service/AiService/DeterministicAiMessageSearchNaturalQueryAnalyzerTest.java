package com.chat.chat.Service.AiService;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicAiMessageSearchNaturalQueryAnalyzerTest {

    private final DeterministicAiMessageSearchNaturalQueryAnalyzer analyzer =
            new DeterministicAiMessageSearchNaturalQueryAnalyzer();

    @Test
    void analyze_localizacionNoExtraeGrupoInventado() {
        AiMessageSearchNaturalQueryAnalysis analysis =
                analyzer.analyze("El otro dia sugeri de ir a japon en que grupo fue");

        assertTrue(analysis.isIntencionLocalizacion());
        assertFalse(analysis.isIntencionGrupo());
        assertNull(analysis.getNombreGrupoDetectado());
        assertNull(analysis.getNombrePersonaDetectado());
    }

    @Test
    void analyze_ultimosDosDiasDetectaRangoSinConfundirloConUltimoMensaje() {
        LocalDateTime before = LocalDateTime.now().minusDays(2).minusMinutes(2);
        AiMessageSearchNaturalQueryAnalysis analysis =
                analyzer.analyze("dame todos los mensajes que he puesto los ultimos 2 dias");
        LocalDateTime after = LocalDateTime.now();

        assertTrue(analysis.isRangoTemporalDetectado());
        assertFalse(analysis.isIntencionUltimoMensaje());
        assertNotNull(analysis.getFechaInicioDetectada());
        assertNotNull(analysis.getFechaFinDetectada());
        assertTrue(!analysis.getFechaInicioDetectada().isBefore(before));
        assertTrue(!analysis.getFechaFinDetectada().isAfter(after));
    }
}
