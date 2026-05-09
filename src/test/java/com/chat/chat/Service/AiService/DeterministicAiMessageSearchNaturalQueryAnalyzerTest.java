package com.chat.chat.Service.AiService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
