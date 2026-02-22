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

    // Rutas Notificaciones
    public static final String NOTIFICACIONES_COUNT = "/count";
    public static final String NOTIFICACIONES_PENDIENTES = "/{userId}/pending";
    public static final String NOTIFICACIONES_RESOLVER = "/{notifId}/resolve";
    public static final String NOTIFICACIONES_VISTA = "/{id}/seen";
    public static final String NOTIFICACIONES_VISTAS_TODAS = "/seen-all";

    // Rutas Usuarios y Auth
    public static final String USUARIOS_ACTIVOS = "/activos";
    public static final String USUARIO_POR_ID = "/{id}";
    public static final String USUARIO_BUSCAR = "/buscar";
    public static final String USUARIO_PUBLIC_KEY = "/{id}/public-key";

    // Rutas Recuperación Contraseña
    public static final String RECUPERAR_PASSWORD_SOLICITAR = "/recuperar-password/solicitar";
    public static final String RECUPERAR_PASSWORD_VERIFICAR = "/recuperar-password/verificar-y-cambiar";

    // Rutas Mensajeria y Chat
    public static final String MENSAJES_GRUPO = "/mensajes/grupo/{chatId}";
    public static final String MENSAJES_MARCAR_LEIDOS = "/mensajes/marcar-leidos";
    public static final String GRUPAL_ADD_USUARIOS = "/{groupId}/usuarios";

    // Rutas Base Restantes (RequestMapping)
    public static final String API_REPORTES = "/api/reportes";
    public static final String API_UPLOADS = "/api/uploads";
    public static final String API_NOTIFICATIONS = "/api/notifications";
    public static final String API_GROUP_INVITES = "/api/group-invites";
    public static final String API_ESTADO = "/api/estado";

    // Subrutas Restantes
    public static final String USUARIOS_SUB = "/usuarios";
    public static final String GROUP_INVITE_ACCEPT = "/{inviteId}/accept";
    public static final String GROUP_INVITE_DECLINE = "/{inviteId}/decline";
    public static final String USUARIO_BLOQUEAR = "/{bloqueadoId}/bloquear";
    public static final String USUARIO_DESBLOQUEAR = "/{bloqueadoId}/desbloquear";

    // Rutas Upload
    public static final String UPLOAD_AUDIO = "/audio";

    // Puedes añadir aquí otras constantes globales:
    public static final String ADMIN = "ADMIN";
    public static final String USUARIO = "USUARIO";

    // Estados de conexión
    public static final String ESTADO_CONECTADO = "Conectado";
    public static final String ESTADO_AUSENTE = "Ausente";
    public static final String ESTADO_DESCONECTADO = "Desconectado";

    // Otros...
}
