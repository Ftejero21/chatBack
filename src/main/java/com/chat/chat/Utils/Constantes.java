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
    public static final String API_UPLOADS_ALL = "/api/uploads";

    // Subrutas Restantes
    public static final String USUARIOS_SUB = "/usuarios";
    public static final String GROUP_INVITE_ACCEPT = "/{inviteId}/accept";
    public static final String GROUP_INVITE_DECLINE = "/{inviteId}/decline";
    public static final String ADMIN_DASHBOARD_STATS = "/admin/dashboard-stats";
    public static final String ADMIN_RECIENTES = "/admin/recientes";
    public static final String ADMIN_USUARIO_CHATS = "/admin/usuario/{id}/chats";
    public static final String ADMIN_USUARIO_BAN = "/admin/{id}/ban";
    public static final String ADMIN_USUARIO_UNBAN = "/admin/{id}/unban";
    public static final String USUARIO_BLOQUEAR = "/{bloqueadoId}/bloquear";
    public static final String USUARIO_DESBLOQUEAR = "/{bloqueadoId}/desbloquear";

    // Rutas Upload
    public static final String UPLOAD_AUDIO = "/audio";

    // Mensajes API
    public static final String KEY_MENSAJE = "mensaje";
    public static final String MSG_USUARIO_BLOQUEADO = "Usuario bloqueado";
    public static final String MSG_USUARIO_DESBLOQUEADO = "Usuario desbloqueado";
    public static final String MSG_EMAIL_REQUERIDO = "Email es requerido";
    public static final String MSG_EMAIL_NO_REGISTRADO = "El email proporcionado no está registrado en el sistema.";
    public static final String MSG_CODIGO_ENVIADO = "Código enviado al correo";
    public static final String MSG_ERROR_ENVIANDO_CORREO = "Error enviando correo";
    public static final String MSG_FALTAN_DATOS_REQUERIDOS = "Faltan datos requeridos";
    public static final String MSG_CODIGO_INVALIDO_O_EXPIRADO = "Código inválido o expirado";
    public static final String MSG_CONTRASENA_ACTUALIZADA = "Contraseña actualizada exitosamente";
    public static final String MSG_USUARIO_REACTIVADO = "Usuario reactivado exitosamente";
    public static final String MSG_REPORTE_LANZADO = "Reporte de usuarios lanzado (soloActivos=";
    public static final String MSG_REPORTE_LANZADO_FIN = ").";
    public static final String MSG_ERROR_LANZAR_REPORTE = "Error al lanzar reporte: ";

    // Claves comunes
    public static final String KEY_UNSEEN_COUNT = "unseenCount";

    // Websocket
    public static final String TOPIC_CALL_SDP_OFFER = "/topic/call.sdp.offer.";
    public static final String TOPIC_CALL_SDP_ANSWER = "/topic/call.sdp.answer.";
    public static final String TOPIC_CALL_ICE = "/topic/call.ice.";
    public static final String TOPIC_CHAT = "/topic/chat.";
    public static final String TOPIC_CHAT_GRUPAL = "/topic/chat.grupal.";
    public static final String TOPIC_ESCRIBIENDO = "/topic/escribiendo.";
    public static final String TOPIC_ESCRIBIENDO_GRUPO = "/topic/escribiendo.grupo.";
    public static final String TOPIC_ESTADO = "/topic/estado.";
    public static final String TOPIC_CALL_INVITE = "/topic/call.invite.";
    public static final String TOPIC_CALL_ANSWER = "/topic/call.answer.";
    public static final String TOPIC_CALL_END = "/topic/call.end.";
    public static final String RINGING = "RINGING";

    // Puedes añadir aquí otras constantes globales:
    public static final String ADMIN = "ADMIN";
    public static final String USUARIO = "USUARIO";
    public static final String DEFAULT_CALLER_NAME = "Usuario";


    public static final String WS_TOPIC_NOTIFICATIONS = "/topic/notifications.";
    public static final String WS_TOPIC_LEIDO = "/topic/leido.";
    public static final String WS_TOPIC_USER_BLOQUEOS_PREFIX = "/topic/user/";
    public static final String WS_TOPIC_USER_BLOQUEOS_SUFFIX = "/bloqueos";
    public static final String WS_QUEUE_BANEOS = "/queue/baneos";

    public static final String DATA_IMAGE_PREFIX = "data:image";
    public static final String DATA_AUDIO_PREFIX = "data:audio";
    public static final String UPLOADS_PREFIX = "/uploads/";
    public static final String HTTP_PREFIX = "http";
    public static final String DIR_GROUP_PHOTOS = "group-photos";
    public static final String DIR_AVATARS = "avatars";
    public static final String DIR_VOICE = "voice";

    public static final String MSG_CHAT_INDIVIDUAL_BLOQUEADO = "No puedes crear un chat individual con un usuario bloqueado";
    public static final String MSG_CREADOR_NO_ENCONTRADO = "Creador no encontrado";
    public static final String MSG_USUARIO_INVITADO_NO_EXISTE = "Usuario invitado no existe: ";
    public static final String MSG_GROUP_ID_OBLIGATORIO = "groupId es obligatorio.";
    public static final String MSG_GRUPO_NO_EXISTE_ID = "No existe el grupo con id: ";
    public static final String MSG_USUARIO_NO_EXISTE_ID = "No existe el usuario con id: ";
    public static final String MSG_NO_PERTENECE_GRUPO = "No perteneces a este grupo.";
    public static final String MSG_SALIO_GRUPO_ELIMINADO = "Has salido del grupo. El grupo se ha eliminado por quedar vacío.";
    public static final String MSG_SALIO_GRUPO = "Has salido del grupo.";
    public static final String MSG_CHAT_GRUPAL_NO_EXISTE = "Chat grupal no existe: ";
    public static final String MSG_INVITADOR_NO_EXISTE = "Invitador no existe: ";
    public static final String MSG_CHAT_NO_ENCONTRADO_ID = "Chat no encontrado con ID: ";
    public static final String MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID = "Chat grupal no encontrado con ID: ";
    public static final String CHAT_TIPO_INDIVIDUAL = "INDIVIDUAL";
    public static final String CHAT_TIPO_GRUPAL = "GRUPAL";
    public static final String MSG_Y = " y ";
    public static final String MSG_GRUPO_SUFFIX = " (Grupo)";
    public static final String MSG_SIN_DATOS = "Sin datos";

    public static final String MSG_NOTIFICACION_NO_EXISTE = "Notificación no existe: ";
    public static final String MSG_CHAT_INDIVIDUAL_NO_ENCONTRADO = "Chat individual no encontrado";
    public static final String MSG_NO_PUEDE_ENVIAR_MENSAJES = "No puedes enviar mensajes en esta conversación";
    public static final String MSG_CHAT_GRUPAL_NO_ENCONTRADO = "Chat grupal no encontrado";
    public static final String KEY_MENSAJE_ID = "mensajeId";

    public static final String MSG_USUARIO_NO_ENCONTRADO = "Usuario no encontrado";
    public static final String MSG_CUENTA_INHABILITADA = "Esta cuenta ha sido inhabilitada por un administrador.";
    public static final String WS_TYPE_BLOCKED = "BLOCKED";
    public static final String WS_TYPE_UNBLOCKED = "UNBLOCKED";
    public static final String BAN_MOTIVO_DEFAULT = "Tu cuenta ha sido suspendida temporalmente por incumplimiento de las normas de uso de TejeChat.";
    public static final String EMAIL_VAR_NOMBRE = "nombre";
    public static final String EMAIL_VAR_MOTIVO = "motivo";
    public static final String EMAIL_SUBJECT_BAN = "Aviso de suspensión de cuenta - TejeChat";
    public static final String EMAIL_TEMPLATE_BAN = "templates/user-banned.html";
    public static final String EMAIL_SUBJECT_UNBAN = "¡Cuenta reactivada! Bienvenido de nuevo - TejeChat";
    public static final String EMAIL_TEMPLATE_UNBAN = "templates/user-unbanned.html";

    // Estados de conexión
    public static final String ESTADO_CONECTADO = "Conectado";
    public static final String ESTADO_AUSENTE = "Ausente";
    public static final String ESTADO_DESCONECTADO = "Desconectado";

    // Otros...
}
