package com.chat.chat.Controller;

import com.chat.chat.DTO.StickerDTO;
import com.chat.chat.Exceptions.ConflictoException;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Security.CorsPreflightDebugFilter;
import com.chat.chat.Security.CustomUserDetailsService;
import com.chat.chat.Security.JwtAuthFilter;
import com.chat.chat.Security.SecurityDebugAccessDeniedHandler;
import com.chat.chat.Security.SecurityDebugAuthenticationEntryPoint;
import com.chat.chat.Service.StickerService.StickerService;
import com.chat.chat.Utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StickerController.class)
class StickerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StickerService stickerService;
    @MockBean
    private SecurityUtils securityUtils;
    @MockBean
    private JwtAuthFilter jwtAuthFilter;
    @MockBean
    private CorsPreflightDebugFilter corsPreflightDebugFilter;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    @MockBean
    private SecurityDebugAuthenticationEntryPoint securityDebugAuthenticationEntryPoint;
    @MockBean
    private SecurityDebugAccessDeniedHandler securityDebugAccessDeniedHandler;

    @Test
    void unauthenticatedEndpoints_return401() throws Exception {
        mockMvc.perform(get("/api/stickers/1/owned-by-me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/stickers/1/save-to-me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void ownedByMe_true_false_404() throws Exception {
        Mockito.when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        Mockito.when(stickerService.isOwnedByUser(10L, 7L)).thenReturn(true);
        mockMvc.perform(get("/api/stickers/10/owned-by-me"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.owned").value(true));

        Mockito.when(stickerService.isOwnedByUser(11L, 7L)).thenReturn(false);
        mockMvc.perform(get("/api/stickers/11/owned-by-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owned").value(false));

        Mockito.when(stickerService.isOwnedByUser(12L, 7L))
                .thenThrow(new RecursoNoEncontradoException("Sticker no encontrado"));
        mockMvc.perform(get("/api/stickers/12/owned-by-me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void saveToMe_ok_duplicate_404() throws Exception {
        Mockito.when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        StickerDTO dto = new StickerDTO();
        dto.setId(55L);
        dto.setNombre("x");
        Mockito.when(stickerService.saveStickerToUser(20L, 7L)).thenReturn(dto);
        mockMvc.perform(post("/api/stickers/20/save-to-me"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55L));

        Mockito.when(stickerService.saveStickerToUser(21L, 7L))
                .thenThrow(new ConflictoException("Sticker ya añadido"));
        mockMvc.perform(post("/api/stickers/21/save-to-me"))
                .andExpect(status().isConflict());

        Mockito.when(stickerService.saveStickerToUser(22L, 7L))
                .thenThrow(new RecursoNoEncontradoException("Sticker no encontrado"));
        mockMvc.perform(post("/api/stickers/22/save-to-me"))
                .andExpect(status().isNotFound());
    }
}
