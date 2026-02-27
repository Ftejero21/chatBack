package com.chat.chat.Configuracion;

import com.chat.chat.Utils.Constantes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.uploads.root:uploads}")
    private String uploadsRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Sirve /uploads/** desde el filesystem
        String location = "file:" + uploadsRoot + "/";
        registry.addResourceHandler(Constantes.UPLOADS_PATTERN)
                .addResourceLocations(location)
                .setCacheControl(CacheControl.noCache()); // ajusta caché si quieres
    }
}
