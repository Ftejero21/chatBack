package com.chat.chat.Utils;

import com.chat.chat.DTO.ChatGrupalDTO;
import com.chat.chat.Entity.ChatGrupalEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingUtilsGroupCloseCompatTest {

    @Test
    void mapeaCamposCompatDeCierreEnChatGrupalDto() {
        ChatGrupalEntity group = new ChatGrupalEntity();
        group.setId(101L);
        group.setNombreGrupo("Equipo");
        group.setUsuarios(List.of());
        group.setClosed(true);
        group.setClosedReason("Mantenimiento");

        ChatGrupalDTO dto = MappingUtils.chatGrupalEntityADto(group);

        assertTrue(Boolean.TRUE.equals(dto.getChatCerrado()));
        assertTrue(Boolean.TRUE.equals(dto.getClosed()));
        assertEquals("Mantenimiento", dto.getChatCerradoMotivo());
        assertEquals("Mantenimiento", dto.getReason());
    }
}
