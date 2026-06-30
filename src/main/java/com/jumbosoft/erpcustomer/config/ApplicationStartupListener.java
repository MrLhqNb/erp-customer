package com.jumbosoft.erpcustomer.config;

import com.jumbosoft.erpcustomer.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartupListener.class);

    private final VectorStoreService vectorStoreService;

    public ApplicationStartupListener(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Initializing vector store connection...");
        try {
            vectorStoreService.ensureCollection();
            log.info("Vector store initialized successfully");
        } catch (Exception e) {
            log.warn("Vector store init warning: {}", e.getMessage());
        }
    }
}
