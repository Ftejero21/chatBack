package com.chat.chat.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

public class UsuarioDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private boolean activo;
    private String foto;
    private String email;
    private String dni;
    private String telefono;
    private String fechaNacimiento;
    private String genero;
    private String direccion;
    private String nacionalidad;
    private String ocupacion;
    private String instagram;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private Set<String> roles;
    private String publicKey;
    private Boolean hasPublicKey;
    private Set<Long> bloqueadosIds;
    private List<BlockedUserDTO> bloqueados;
    private Set<Long> meHanBloqueadoIds;

    public Set<Long> getMeHanBloqueadoIds() {
        return meHanBloqueadoIds;
    }

    public void setMeHanBloqueadoIds(Set<Long> meHanBloqueadoIds) {
        this.meHanBloqueadoIds = meHanBloqueadoIds;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public Boolean getHasPublicKey() {
        return hasPublicKey;
    }

    public void setHasPublicKey(Boolean hasPublicKey) {
        this.hasPublicKey = hasPublicKey;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }

    public String getOcupacion() {
        return ocupacion;
    }

    public void setOcupacion(String ocupacion) {
        this.ocupacion = ocupacion;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public boolean isActivo() {
        return activo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<Long> getBloqueadosIds() {
        return bloqueadosIds;
    }

    public void setBloqueadosIds(Set<Long> bloqueadosIds) {
        this.bloqueadosIds = bloqueadosIds;
    }

    public List<BlockedUserDTO> getBloqueados() {
        return bloqueados;
    }

    public void setBloqueados(List<BlockedUserDTO> bloqueados) {
        this.bloqueados = bloqueados;
    }
}
