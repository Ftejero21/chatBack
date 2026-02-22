package com.chat.chat.Utils;

import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Devuelve el ID del usuario actualmente autenticado (el que envió el JWT).
     * Útil para evitar Spoofing e IDOR, ya que la fuente de verdad es el Token, no
     * lo que mande el cliente.
     */
    public Long getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        UsuarioEntity user = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en DB"));

        return user.getId();
    }
}
