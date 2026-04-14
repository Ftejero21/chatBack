package com.chat.chat.Service.PresenceService;

import com.chat.chat.Configuracion.EstadoUsuarioManager;
import com.chat.chat.Utils.Constantes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPresenceServiceTest {

    @Mock
    private PresenceBroadcastService presenceBroadcastService;

    @Mock
    private EstadoUsuarioManager estadoUsuarioManager;

    @InjectMocks
    private UserPresenceService userPresenceService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(userPresenceService, "awayTimeoutMs", 60L);
        when(presenceBroadcastService.normalizeEstado(anyString())).thenCallRealMethod();
    }

    @Test
    void publicaSoloCuandoHayTransicionReal() {
        Long userId = 10L;
        String sessionId = "s-1";

        userPresenceService.registerSessionConnected(userId, sessionId);
        userPresenceService.handlePresenceSignal(userId, sessionId, Constantes.ESTADO_CONECTADO);
        userPresenceService.handlePresenceSignal(userId, sessionId, Constantes.ESTADO_CONECTADO);

        verify(presenceBroadcastService, times(1))
                .publishPresenceToAuthorized(eq(userId), eq(Constantes.ESTADO_CONECTADO), eq("-"));
        verify(estadoUsuarioManager, times(1)).marcarConectado(userId);
        verify(estadoUsuarioManager, never()).marcarDesconectado(userId);
    }

    @Test
    void pasaAAusentePorInactividadEnNoVisibleYVuelveAConectado() throws InterruptedException {
        Long userId = 11L;
        String sessionId = "s-2";

        userPresenceService.registerSessionConnected(userId, sessionId);
        userPresenceService.handlePresenceSignal(userId, sessionId, Constantes.ESTADO_AUSENTE);
        Thread.sleep(90L);
        userPresenceService.reconcilePresenceStates();
        userPresenceService.handlePresenceSignal(userId, sessionId, Constantes.ESTADO_CONECTADO);

        verify(presenceBroadcastService, times(1))
                .publishPresenceToAuthorized(eq(userId), eq(Constantes.ESTADO_AUSENTE), eq("-"));
        verify(presenceBroadcastService, times(2))
                .publishPresenceToAuthorized(eq(userId), eq(Constantes.ESTADO_CONECTADO), eq("-"));
    }

    @Test
    void desconectaAlCerrarSesionWebSocket() {
        Long userId = 12L;
        String sessionId = "s-3";

        userPresenceService.registerSessionConnected(userId, sessionId);
        userPresenceService.registerSessionDisconnected(userId, sessionId);

        verify(presenceBroadcastService, times(1))
                .publishPresenceToAuthorized(eq(userId), eq(Constantes.ESTADO_CONECTADO), eq("-"));
        verify(presenceBroadcastService, times(1))
                .publishPresenceToAuthorized(eq(userId), eq(Constantes.ESTADO_DESCONECTADO), eq("-"));
        verify(estadoUsuarioManager, times(1)).marcarConectado(userId);
        verify(estadoUsuarioManager, times(1)).marcarDesconectado(userId);
    }
}
