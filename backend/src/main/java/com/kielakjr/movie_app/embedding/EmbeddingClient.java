package com.kielakjr.movie_app.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EmbeddingClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIMEOUT)
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String embeddingUrl;
    private final String batchEmbeddingUrl;

    public EmbeddingClient(@Value("${embedding.base-url}") String baseUrl) {
        this.embeddingUrl = baseUrl + "/embedding";
        this.batchEmbeddingUrl = baseUrl + "/embedding/batch";
    }

    public float[] embed(String text) {
        try {
            String body = OBJECT_MAPPER.writeValueAsString(Map.of("text", text));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingUrl))
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Embedding service error {}: {}", response.statusCode(), response.body());
                return new float[0];
            }

            EmbedResponse parsed = OBJECT_MAPPER.readValue(response.body(), EmbedResponse.class);
            return toFloatArray(parsed.embedding());
        } catch (Exception e) {
            log.error("Failed to get embedding: {}", e.getMessage(), e);
            return new float[0];
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        try {
            String body = OBJECT_MAPPER.writeValueAsString(Map.of("texts", texts));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(batchEmbeddingUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Embedding batch service error {}: {}", response.statusCode(), response.body());
                return List.of();
            }

            BatchEmbedResponse parsed = OBJECT_MAPPER.readValue(response.body(), BatchEmbedResponse.class);
            List<float[]> result = new ArrayList<>(parsed.embeddings().size());
            for (List<Double> embedding : parsed.embeddings()) {
                result.add(toFloatArray(embedding));
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to get batch embeddings: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private static float[] toFloatArray(List<Double> doubles) {
        float[] arr = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) arr[i] = doubles.get(i).floatValue();
        return arr;
    }

    record EmbedResponse(List<Double> embedding) {}

    record BatchEmbedResponse(List<List<Double>> embeddings) {}
}
