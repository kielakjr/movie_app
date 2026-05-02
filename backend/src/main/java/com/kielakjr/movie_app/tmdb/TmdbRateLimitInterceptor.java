package com.kielakjr.movie_app.tmdb;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import java.io.IOException;
import io.github.bucket4j.Bucket;

@Component
@RequiredArgsConstructor
public class TmdbRateLimitInterceptor implements ClientHttpRequestInterceptor {

    private final Bucket tmdbBucket;

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        try {
            tmdbBucket.asBlocking().consume(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for TMDB rate limit token", e);
        }

        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode().value() == 429) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during TMDB 429 backoff", e);
            }
            return execution.execute(request, body);
        }

        return response;
    }
}
