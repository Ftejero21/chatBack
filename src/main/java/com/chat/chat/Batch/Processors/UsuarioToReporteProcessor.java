package com.chat.chat.Batch.Processors;

import com.chat.chat.DTO.UsuarioReporteCsvDTO;
import com.chat.chat.Entity.UsuarioEntity;
import org.springframework.batch.item.ItemProcessor;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class UsuarioToReporteProcessor implements ItemProcessor<UsuarioEntity, UsuarioReporteCsvDTO> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private boolean soloActivos;

    public void setSoloActivos(boolean soloActivos) { this.soloActivos = soloActivos; }

    @Override
    public UsuarioReporteCsvDTO process(UsuarioEntity u) {
        if (soloActivos && !u.isActivo()) return null;

        UsuarioReporteCsvDTO dto = new UsuarioReporteCsvDTO();
        dto.setId(u.getId());
        dto.setNombre(safe(u.getNombre()));
        dto.setApellido(safe(u.getApellido()));
        dto.setEmail(lower(u.getEmail()));
        dto.setActivo(u.isActivo() ? "SI" : "NO");
        dto.setFechaCreacion(u.getFechaCreacion() != null ? u.getFechaCreacion().format(FMT) : "");
        dto.setRoles(u.getRoles() == null ? "" : u.getRoles().stream().sorted().collect(Collectors.joining(",")));
        dto.setFotoUrl(safe(u.getFotoUrl()));
        return dto;
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
    private String lower(String s) { return s == null ? "" : s.trim().toLowerCase(); }
}
