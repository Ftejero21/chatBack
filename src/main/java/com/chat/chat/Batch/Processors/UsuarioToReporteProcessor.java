package com.chat.chat.Batch.Processors;

import com.chat.chat.DTO.UsuarioReporteCsvDTO;
import com.chat.chat.Entity.UsuarioEntity;
import org.springframework.batch.item.ItemProcessor;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class UsuarioToReporteProcessor implements ItemProcessor<UsuarioEntity, UsuarioReporteCsvDTO> {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";
    private static final String ACTIVE_YES = "SI";
    private static final String ACTIVE_NO = "NO";
    private static final String EMPTY = "";
    private static final String ROLES_DELIMITER = ",";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern(DATE_FORMAT);
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
        dto.setActivo(u.isActivo() ? ACTIVE_YES : ACTIVE_NO);
        dto.setFechaCreacion(u.getFechaCreacion() != null ? u.getFechaCreacion().format(FMT) : EMPTY);
        dto.setRoles(u.getRoles() == null ? EMPTY : u.getRoles().stream().sorted().collect(Collectors.joining(ROLES_DELIMITER)));
        dto.setFotoUrl(safe(u.getFotoUrl()));
        return dto;
    }

    private String safe(String s) { return s == null ? EMPTY : s.trim(); }
    private String lower(String s) { return s == null ? EMPTY : s.trim().toLowerCase(); }
}
