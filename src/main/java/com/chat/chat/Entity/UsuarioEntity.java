package com.chat.chat.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = false, nullable = true)
    private String nombre;

    @Column(unique = true, nullable = false)
    private String email;
    private String apellido;
    private String password;

    private LocalDateTime fechaCreacion;
    private LocalDateTime publicKeyUpdatedAt;
    private boolean activo;
    private Boolean emailVerificado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "rol")
    private Set<String> roles = new HashSet<>();

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "dni", length = 16)
    private String dni;

    @Column(name = "telefono", length = 32)
    private String telefono;

    @Column(name = "fecha_nacimiento", length = 32)
    private String fechaNacimiento;

    @Column(name = "genero", length = 32)
    private String genero;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "nacionalidad", length = 80)
    private String nacionalidad;

    @Column(name = "ocupacion", length = 120)
    private String ocupacion;

    @Column(name = "instagram", length = 120)
    private String instagram;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String publicKey;

    @OneToMany(mappedBy = "emisor", cascade = CascadeType.ALL)
    private List<MensajeEntity> mensajes = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_bloqueados", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "bloqueado_id"))
    private Set<UsuarioEntity> bloqueados = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getPublicKeyUpdatedAt() {
        return publicKeyUpdatedAt;
    }

    public void setPublicKeyUpdatedAt(LocalDateTime publicKeyUpdatedAt) {
        this.publicKeyUpdatedAt = publicKeyUpdatedAt;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Boolean getEmailVerificado() {
        return emailVerificado;
    }

    public void setEmailVerificado(Boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public List<MensajeEntity> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<MensajeEntity> mensajes) {
        this.mensajes = mensajes;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<UsuarioEntity> getBloqueados() {
        return bloqueados;
    }

    public void setBloqueados(Set<UsuarioEntity> bloqueados) {
        this.bloqueados = bloqueados;
    }

    @ManyToMany(mappedBy = "bloqueados", fetch = FetchType.LAZY)
    private Set<UsuarioEntity> meHanBloqueado = new HashSet<>();

    public Set<UsuarioEntity> getMeHanBloqueado() {
        return meHanBloqueado;
    }

    public void setMeHanBloqueado(Set<UsuarioEntity> meHanBloqueado) {
        this.meHanBloqueado = meHanBloqueado;
    }
}
