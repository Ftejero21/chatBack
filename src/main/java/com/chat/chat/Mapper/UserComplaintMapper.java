package com.chat.chat.Mapper;

import com.chat.chat.DTO.UserComplaintDTO;
import com.chat.chat.DTO.UserComplaintHistoryItemDTO;
import com.chat.chat.DTO.UserComplaintWsDTO;
import com.chat.chat.Entity.UserComplaintHistoryEntity;
import com.chat.chat.Entity.UserComplaintEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;

@Component
public class UserComplaintMapper {

    public UserComplaintDTO toDto(UserComplaintEntity entity) {
        return toDto(entity, null);
    }

    public UserComplaintDTO toDto(UserComplaintEntity entity, List<UserComplaintHistoryItemDTO> historialDenuncia) {
        UserComplaintDTO dto = new UserComplaintDTO();
        dto.setId(entity.getId());
        dto.setDenuncianteId(entity.getDenuncianteId());
        dto.setDenunciadoId(entity.getDenunciadoId());
        dto.setChatId(entity.getChatId());
        dto.setMotivo(entity.getMotivo());
        dto.setDetalle(entity.getDetalle());
        dto.setEstado(entity.getEstado());
        dto.setLeida(entity.isLeida());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setLeidaAt(entity.getLeidaAt());
        dto.setDenuncianteNombre(entity.getDenuncianteNombre());
        dto.setDenunciadoNombre(entity.getDenunciadoNombre());
        dto.setChatNombreSnapshot(entity.getChatNombreSnapshot());
        dto.setReviewedByAdminId(entity.getReviewedByAdminId());
        dto.setHistorialDenuncia(historialDenuncia);
        return dto;
    }

    public UserComplaintWsDTO toWsDto(String event, UserComplaintEntity entity) {
        UserComplaintWsDTO dto = new UserComplaintWsDTO();
        dto.setEvent(event);
        dto.setId(entity.getId());
        dto.setDenuncianteId(entity.getDenuncianteId());
        dto.setDenunciadoId(entity.getDenunciadoId());
        dto.setChatId(entity.getChatId());
        dto.setMotivo(entity.getMotivo());
        dto.setDetalle(entity.getDetalle());
        dto.setEstado(entity.getEstado());
        dto.setLeida(entity.isLeida());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setLeidaAt(entity.getLeidaAt());
        dto.setDenuncianteNombre(entity.getDenuncianteNombre());
        dto.setDenunciadoNombre(entity.getDenunciadoNombre());
        dto.setChatNombreSnapshot(entity.getChatNombreSnapshot());
        dto.setReviewedByAdminId(entity.getReviewedByAdminId());
        return dto;
    }

    public UserComplaintHistoryItemDTO toHistoryDto(UserComplaintHistoryEntity entity) {
        UserComplaintHistoryItemDTO dto = new UserComplaintHistoryItemDTO();
        dto.setEstadoAnterior(entity.getEstadoAnterior() == null ? null : entity.getEstadoAnterior().name());
        dto.setEstadoNuevo(entity.getEstadoNuevo() == null ? null : entity.getEstadoNuevo().name());
        dto.setEstadoLabel(entity.getEstadoLabel());
        dto.setMotivo(entity.getMotivoSnapshot());
        dto.setDetalle(entity.getDetalleSnapshot());
        dto.setResolucionMotivo(entity.getResolucionMotivo());
        dto.setFecha(entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC).toInstant().toString());
        dto.setAdminId(entity.getAdminId());
        dto.setAccion(entity.getAccion());
        return dto;
    }
}
