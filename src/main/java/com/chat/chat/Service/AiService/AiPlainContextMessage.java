package com.chat.chat.Service.AiService;

class AiPlainContextMessage {

    private Long id;
    private Long autorId;
    private String autor;
    private String fecha;
    private boolean esUsuarioActual;
    private String contenido;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public boolean isEsUsuarioActual() {
        return esUsuarioActual;
    }

    public void setEsUsuarioActual(boolean esUsuarioActual) {
        this.esUsuarioActual = esUsuarioActual;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}
