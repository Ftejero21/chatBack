package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.DTO.AiEncryptedMessageSearchResultDTO;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.MensajeProgramadoRepository;
import com.chat.chat.Repository.SolicitudDesbaneoRepository;
import com.chat.chat.Repository.SolicitudReporteHistorialRepository;
import com.chat.chat.Repository.UserComplaintRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Service.SolicitudDesbaneoService.SolicitudDesbaneoService;
import com.chat.chat.Utils.AdminAuditCrypto;
import com.chat.chat.Utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeepSeekAiEncryptedMessageSearchServiceImplResumenBusquedaTest {

    private UsuarioRepository usuarioRepository;
    private DeepSeekAiEncryptedMessageSearchServiceImpl service;
    private Method construirResumenBusquedaMethod;

    @BeforeEach
    void setUp() throws Exception {
        usuarioRepository = mock(UsuarioRepository.class);

        service = new DeepSeekAiEncryptedMessageSearchServiceImpl(
                new AiProperties(),
                mock(AiRateLimitService.class),
                mock(SecurityUtils.class),
                mock(ChatIndividualRepository.class),
                mock(ChatGrupalRepository.class),
                mock(MensajeRepository.class),
                mock(MensajeProgramadoRepository.class),
                usuarioRepository,
                mock(UserComplaintRepository.class),
                mock(AiEncryptedContextService.class),
                mock(AdminAuditCrypto.class),
                mock(AudioTranscriptionService.class),
                mock(AiMessageSearchNaturalQueryAnalyzer.class),
                mock(AiMessageSearchScopeResolverService.class),
                mock(AiMessageSearchMicroserviceClient.class),
                mock(AiSearchIntentMicroserviceClient.class),
                mock(AiScheduledMessageSummaryMicroserviceClient.class),
                mock(SolicitudDesbaneoService.class),
                mock(SolicitudDesbaneoRepository.class),
                mock(SolicitudReporteHistorialRepository.class),
                mock(AiAppReportStatusSummaryMicroserviceClient.class),
                mock(AiAppReportResolutionNoteMicroserviceClient.class),
                mock(AiSearchProgressNotifier.class),
                new ObjectMapper(),
                "uploads",
                "",
                "",
                "localhost",
                "8080"
        );

        construirResumenBusquedaMethod = DeepSeekAiEncryptedMessageSearchServiceImpl.class.getDeclaredMethod(
                "construirResumenBusqueda",
                String.class,
                AiMessageSearchNaturalQueryAnalysis.class,
                AiMessageSearchScopeType.class,
                String.class,
                String.class,
                boolean.class,
                AiMessageSearchScopeDTO.class,
                List.class,
                boolean.class,
                boolean.class,
                String.class,
                boolean.class,
                Long.class
        );
        construirResumenBusquedaMethod.setAccessible(true);
    }

    @Test
    void resumenBusqueda_haceMismatchHumanoConAudio() throws Exception {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario("Fernando")));

        String resumen = invokeResumen(
                "el otro dia le mande un mensaje a mariano insultandole o era a daniel no lo se buscalo pls",
                analysis(false),
                AiMessageSearchScopeType.INDIVIDUAL,
                "Mariano",
                "Mariano",
                true,
                scope(AiMessageSearchScopeType.INDIVIDUAL),
                List.of(resultado("Daniel", null, "AUDIO", 82)),
                false,
                false,
                null,
                false,
                7L
        );

        assertNotNull(resumen);
        String normalized = resumen.toLowerCase();
        assertTrue(normalized.contains("fernando"));
        assertTrue(normalized.contains("mariano"));
        assertTrue(normalized.contains("daniel"));
        assertTrue(normalized.contains("audio"));
    }

    @Test
    void resumenBusqueda_mencionaBusquedaAmpliadaCuandoHayFallbackGlobal() throws Exception {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario("Fernando")));

        String resumen = invokeResumen(
                "busca el mensaje con mariano",
                analysis(false),
                AiMessageSearchScopeType.INDIVIDUAL,
                "Mariano",
                "Mariano",
                true,
                scope(AiMessageSearchScopeType.GLOBAL),
                List.of(resultado("Daniel", null, "TEXT", 78)),
                true,
                false,
                "TEXT",
                false,
                7L
        );

        assertNotNull(resumen);
        String normalized = resumen.toLowerCase();
        assertTrue(normalized.contains("busqueda") || normalized.contains("busqué") || normalized.contains("amplie"));
        assertTrue(normalized.contains("daniel"));
    }

    @Test
    void resumenBusqueda_confirmaCoincidenciaCuandoEraElChatCorrecto() throws Exception {
        String resumen = invokeResumen(
                "busca el mensaje con daniel",
                analysis(false),
                AiMessageSearchScopeType.INDIVIDUAL,
                "Daniel",
                "Daniel",
                true,
                scope(AiMessageSearchScopeType.INDIVIDUAL),
                List.of(resultado("Daniel", null, "TEXT", 95)),
                false,
                false,
                "TEXT",
                false,
                7L
        );

        assertNotNull(resumen);
        String normalized = resumen.toLowerCase();
        assertTrue(normalized.contains("chat"));
        assertTrue(normalized.contains("coincidencia") || normalized.contains("encaja"));
    }

    @Test
    void resumenBusqueda_enConsultaDeLocalizacionSoloExplicaDondeFue() throws Exception {
        AiMessageSearchNaturalQueryAnalysis analysis = analysis(false);
        analysis.setIntencionLocalizacion(true);

        String resumen = invokeResumen(
                "El otro dia sugeri de ir a japon en que grupo fue",
                analysis,
                AiMessageSearchScopeType.GLOBAL,
                null,
                null,
                false,
                scope(AiMessageSearchScopeType.GLOBAL),
                List.of(resultado(null, "otra prueba", "TEXT", 88)),
                false,
                false,
                "TEXT",
                false,
                7L
        );

        assertNotNull(resumen);
        String normalized = resumen.toLowerCase();
        assertTrue(normalized.contains("otra prueba"));
        assertFalse(normalized.contains("no sale con"));
        assertFalse(normalized.contains("confundiste"));
        assertFalse(normalized.contains("japon"));
    }

    private String invokeResumen(String consulta,
                                 AiMessageSearchNaturalQueryAnalysis analysis,
                                 AiMessageSearchScopeType scopeInicialType,
                                 String nombreScopeInicial,
                                 String nombreDetectadoInicial,
                                 boolean intencionPersonaOGrupoInicial,
                                 AiMessageSearchScopeDTO scopeFinal,
                                 List<AiEncryptedMessageSearchResultDTO> resultados,
                                 boolean fallbackScopeGlobal,
                                 boolean fallbackSinRangoTemporal,
                                 String requestedType,
                                 boolean intencionAudioDetectada,
                                 Long userId) throws Exception {
        return (String) construirResumenBusquedaMethod.invoke(
                service,
                consulta,
                analysis,
                scopeInicialType,
                nombreScopeInicial,
                nombreDetectadoInicial,
                intencionPersonaOGrupoInicial,
                scopeFinal,
                resultados,
                fallbackScopeGlobal,
                fallbackSinRangoTemporal,
                requestedType,
                intencionAudioDetectada,
                userId
        );
    }

    private static AiMessageSearchNaturalQueryAnalysis analysis(boolean intencionAudio) {
        AiMessageSearchNaturalQueryAnalysis analysis = new AiMessageSearchNaturalQueryAnalysis();
        analysis.setIntencionAudio(intencionAudio);
        return analysis;
    }

    private static AiMessageSearchScopeDTO scope(AiMessageSearchScopeType type) {
        AiMessageSearchScopeDTO scope = new AiMessageSearchScopeDTO();
        scope.setTipoScope(type);
        return scope;
    }

    private static AiEncryptedMessageSearchResultDTO resultado(String receptor, String grupo, String tipo, int relevancia) {
        AiEncryptedMessageSearchResultDTO result = new AiEncryptedMessageSearchResultDTO();
        result.setNombreReceptor(receptor);
        result.setNombreChatGrupal(grupo);
        result.setTipoMensaje(tipo);
        result.setRelevancia(relevancia);
        return result;
    }

    private static UsuarioEntity usuario(String nombre) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombre(nombre);
        return usuario;
    }
}
