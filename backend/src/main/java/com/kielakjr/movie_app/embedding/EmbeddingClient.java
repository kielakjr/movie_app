package com.kielakjr.movie_app.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class EmbeddingClient {
    private final RestClient restClient;

    public EmbeddingClient(@Value("${embedding.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<float[]> embed(List<String> texts) {
        var response = restClient.post()
                .uri("/embed")
                .body(new EmbedRequest(texts))
                .retrieve()
                .body(EmbedResponse.class);
        return response.embeddings().stream()
                .map(EmbeddingClient::toFloatArray)
                .toList();
    }

    private static float[] toFloatArray(List<Double> doubles) {
        float[] arr = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) arr[i] = doubles.get(i).floatValue();
        return arr;
    }

    record EmbedRequest(List<String> texts) {}
    record EmbedResponse(List<List<Double>> embeddings) {}
}
