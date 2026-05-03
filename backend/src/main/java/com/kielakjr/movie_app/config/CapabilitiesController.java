package com.kielakjr.movie_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/capabilities")
public class CapabilitiesController {

    @Value("${embedding.enabled:true}")
    private boolean embeddingEnabled;

    @GetMapping
    public Map<String, Boolean> capabilities() {
        return Map.of("semantic_search", embeddingEnabled);
    }
}
