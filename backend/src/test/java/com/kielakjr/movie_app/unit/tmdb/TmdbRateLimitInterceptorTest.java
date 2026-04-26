package com.kielakjr.movie_app.unit.tmdb;

import com.kielakjr.movie_app.tmdb.TmdbRateLimitInterceptor;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TmdbRateLimitInterceptorTest {

    @Mock
    private Bucket bucket;

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    private TmdbRateLimitInterceptor interceptor;

    private static final byte[] BODY = new byte[0];

    @BeforeEach
    void setUp() {
        interceptor = new TmdbRateLimitInterceptor(bucket);
    }

    @Nested
    class WhenTokenIsAvailable {

        @Test
        void executesRequestOnceAndReturnsResponse() throws IOException {
            when(bucket.tryConsume(1)).thenReturn(true);
            when(execution.execute(request, BODY)).thenReturn(response);
            when(response.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));

            ClientHttpResponse result = interceptor.intercept(request, BODY, execution);

            assertThat(result).isSameAs(response);
            verify(execution, times(1)).execute(request, BODY);
        }
    }

    @Nested
    class WhenResponseIs429 {

        @Test
        void retriesRequestAndReturnsSecondResponse() throws IOException {
            ClientHttpResponse retryResponse = mock(ClientHttpResponse.class);

            when(bucket.tryConsume(1)).thenReturn(true);
            when(execution.execute(request, BODY)).thenReturn(response, retryResponse);
            when(response.getStatusCode()).thenReturn(HttpStatusCode.valueOf(429));

            ClientHttpResponse result = interceptor.intercept(request, BODY, execution);

            assertThat(result).isSameAs(retryResponse);
            verify(execution, times(2)).execute(request, BODY);
        }
    }

    @Nested
    class WhenInterruptedDuringSpin {

        @Test
        void setsInterruptFlagAndThrowsIOException() throws IOException {
            Thread testThread = Thread.currentThread();

            when(bucket.tryConsume(1))
                .thenAnswer(inv -> {
                    testThread.interrupt();
                    return false;
                });

            assertThatThrownBy(() -> interceptor.intercept(request, BODY, execution))
                .isInstanceOf(IOException.class)
                .hasCauseInstanceOf(InterruptedException.class);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();

            Thread.interrupted();
        }
    }
}
