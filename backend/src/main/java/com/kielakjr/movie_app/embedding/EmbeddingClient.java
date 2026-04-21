package com.kielakjr.movie_app.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EmbeddingClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final ObjectMapper objectMapper;
    private final String embeddingUrl;

    public EmbeddingClient(ObjectMapper objectMapper, @Value("${embedding.base-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.embeddingUrl = baseUrl + "/embedding";
    }

    public float[] embed(String text) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("text", text));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Embedding service error {}: {}", response.statusCode(), response.body());
                return new float[0];
            }

            EmbedResponse parsed = objectMapper.readValue(response.body(), EmbedResponse.class);
            return toFloatArray(parsed.embedding());
        } catch (Exception e) {
            log.error("Failed to get embedding: {}", e.getMessage(), e);
            return new float[0];
        }
    }

    private static float[] toFloatArray(List<Double> doubles) {
        float[] arr = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) arr[i] = doubles.get(i).floatValue();
        return arr;
    }

    record EmbedResponse(List<Double> embedding) {}
}
