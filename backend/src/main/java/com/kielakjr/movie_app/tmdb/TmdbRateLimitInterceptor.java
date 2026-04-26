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

        while (!tmdbBucket.tryConsume(1)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
        }

        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode().value() == 429) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return execution.execute(request, body);
        }

        return response;
    }
}
