package com.chat.chat.DTO;

public class AdminGroupMetricDTO {

    private Long grupoId;
    private String nombreGrupo;
    private long totalMensajes;
    private long totalMiembros;

    public Long getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(Long grupoId) {
        this.grupoId = grupoId;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public void setNombreGrupo(String nombreGrupo) {
        this.nombreGrupo = nombreGrupo;
    }

    public long getTotalMensajes() {
        return totalMensajes;
    }

    public void setTotalMensajes(long totalMensajes) {
        this.totalMensajes = totalMensajes;
    }

    public long getTotalMiembros() {
        return totalMiembros;
    }

    public void setTotalMiembros(long totalMiembros) {
        this.totalMiembros = totalMiembros;
    }
}
