package com.chat.chat.Configuracion;

import com.chat.chat.Utils.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketSecurityInterceptor webSocketSecurityInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(Constantes.WS_ENDPOINT)
                .setAllowedOriginPatterns(Constantes.CORS_ANY_ORIGIN)
                .withSockJS() // ← SockJS habilitado
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30_000);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Destino para mensajes que se envían desde el cliente (app/chat.send)
        registry.setApplicationDestinationPrefixes(Constantes.WS_APP_PREFIX);

        // Destino al que el cliente se suscribe para recibir mensajes (topic/chat.{id})
        // Añadido /queue para permitir a Spring enrutar convertAndSendToUser
        registry.enableSimpleBroker(Constantes.WS_BROKER_TOPIC, Constantes.WS_BROKER_QUEUE);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry
                .setMessageSizeLimit(256 * 1024) // 256 KB (ajusta a gusto)
                .setSendBufferSizeLimit(512 * 1024) // buffer envío
                .setSendTimeLimit(25_000); // 25s
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketSecurityInterceptor);
    }
}
