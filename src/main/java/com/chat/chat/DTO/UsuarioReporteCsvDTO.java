package com.chat.chat.DTO;

public class UsuarioReporteCsvDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String activo;         // "SI" / "NO"
    private String fechaCreacion;  // formateada: yyyy-MM-dd HH:mm
    private String roles;          // "ADMIN,USER"
    private String fotoUrl;        // si la tienes pública

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getActivo() { return activo; }
    public void setActivo(String activo) { this.activo = activo; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
}
