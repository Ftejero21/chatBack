package com.chat.chat.DTO;

import java.time.LocalDateTime;

public class ChatResumenDTO {

    private Long id;
    private String nombreChat; // Nombre del grupo o nombre del "otro" usuario
    private String tipo; // "INDIVIDUAL" o "GRUPAL"
    private String ultimoMensaje;
    private LocalDateTime fechaUltimoMensaje;
    private int totalMensajes;
	
    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getNombreChat() {
		return nombreChat;
	}
	public void setNombreChat(String nombreChat) {
		this.nombreChat = nombreChat;
	}
	public String getTipo() {
		return tipo;
	}
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}
	public String getUltimoMensaje() {
		return ultimoMensaje;
	}
	public void setUltimoMensaje(String ultimoMensaje) {
		this.ultimoMensaje = ultimoMensaje;
	}
	public LocalDateTime getFechaUltimoMensaje() {
		return fechaUltimoMensaje;
	}
	public void setFechaUltimoMensaje(LocalDateTime fechaUltimoMensaje) {
		this.fechaUltimoMensaje = fechaUltimoMensaje;
	}
	public int getTotalMensajes() {
		return totalMensajes;
	}
	public void setTotalMensajes(int totalMensajes) {
		this.totalMensajes = totalMensajes;
	}

    
    
}
