package com.chat.chat.Utils;

public class ExceptionConstants {

    // Errores de Roles y Permisos
    public static final String ERROR_NOT_AUTHORIZED_RESOLVE = "No autorizado para resolver esta notificación";
    public static final String ERROR_NOT_AUTHORIZED_MARK = "No autorizado para marcar esta notificación";
    public static final String ERROR_NOT_AUTHORIZED_PUBLIC_KEY = "No tienes permiso para actualizar esta llave pública";
    public static final String ERROR_CREATE_THIRD_PARTY_CHAT = "No tienes permiso para crear un chat entre dos terceros.";
    public static final String ERROR_NOT_GROUP_MEMBER = "El invitador no pertenece al grupo";
    public static final String ERROR_INVITE_NOT_FOR_USER = "Esta invitación no corresponde al usuario";

    // Errores de Usuarios
    public static final String ERROR_EMAIL_EXISTS = "Ya existe un usuario con ese email";
    public static final String ERROR_CANT_BLOCK_SELF = "No puedes bloquearte a ti mismo";

    // Errores de Uploads y Archivos
    public static final String ERROR_AUDIO_SAVE_FAILED = "Fallo al almacenar el archivo de audio";
    public static final String ERROR_AUDIO_EMPTY = "El archivo de audio no puede estar vacío.";
    public static final String ERROR_FILE_TYPE_NOT_ALLOWED = "Tipo de archivo no permitido: ";
    public static final String ERROR_FILE_SIZE_EXCEEDED = "El archivo excede el tamaño máximo permitido.";

    public static final String ERROR_LANZAR_REPORTE = "Error al lanzar reporte: ";
}
