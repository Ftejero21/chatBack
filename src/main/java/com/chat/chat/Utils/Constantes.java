package com.chat.chat.Utils;

public class Constantes {

    // Rutas API Chat
    public static final String API_CHAT = "/api/chat";

    public static final String API_MENSAJERIA = "/api/mensajeria";

    public static final String LISTAR_MENSAJES_CHAT = "/mensajes";

    public static final String USUARIO_API = "/api/usuarios";

    public static final String REGISTRO = "/registro";

    public static final String INDIVIDUAL = "/individual";
    public static final String GRUPAL = "/grupal";

    public static final String LOGIN = "/login";

    public static final String GRUPAL_SALIR = GRUPAL + "/salir";

    public static final String GRUPAL_ES_MIEMBRO = GRUPAL + "/{groupId}/es-miembro/{userId}";
    public static final String CHATS_USUARIO = "/usuario/{usuarioId}/todos";
    public static final String GRUPALES_USUARIO = "/grupal/usuario/{usuarioId}";

    // Puedes añadir aquí otras constantes globales:
    public static final String ADMIN = "ADMIN";
    public static final String USUARIO = "USUARIO";

    // Estados de conexión
    public static final String ESTADO_CONECTADO = "Conectado";
    public static final String ESTADO_AUSENTE = "Ausente";
    public static final String ESTADO_DESCONECTADO = "Desconectado";

    // Otros...
}
