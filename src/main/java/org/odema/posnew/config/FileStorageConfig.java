package org.odema.posnew.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    @Value("${app.file.storage-path:uploads}")
    private String storagePath;

    @Value("${app.file.max-size:10485760}") // 10MB
    private long maxFileSize;

    @Value("${app.file.allowed-extensions:jpg,jpeg,png,gif,pdf,doc,docx}")
    private String[] allowedExtensions;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + storagePath + "/");
    }
}