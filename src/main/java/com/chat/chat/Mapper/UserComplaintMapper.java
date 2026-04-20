package com.chat.chat.Mapper;

import com.chat.chat.DTO.UserComplaintDTO;
import com.chat.chat.DTO.UserComplaintWsDTO;
import com.chat.chat.Entity.UserComplaintEntity;
import org.springframework.stereotype.Component;

@Component
public class UserComplaintMapper {

    public UserComplaintDTO toDto(UserComplaintEntity entity) {
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
        return dto;
    }
}
