package com.kielakjr.movie_app.embedding;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class EmbeddingClientTest {

    private HttpServer server;
    private EmbeddingClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        client = new EmbeddingClient("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void registerHandler(int status, String body) {
        server.createContext("/embedding", exchange -> {
            byte[] bytes = body.getBytes();
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    @Nested
    class WhenServiceResponds {

        @Test
        void success_returnsFloatArrayWithCorrectValues() {
            registerHandler(200, """
                    {"embedding": [0.1, 0.2, 0.3]}
                    """);

            float[] result = client.embed("some text");

            assertThat(result).hasSize(3);
            assertThat(result[0]).isCloseTo(0.1f, offset(0.0001f));
            assertThat(result[1]).isCloseTo(0.2f, offset(0.0001f));
            assertThat(result[2]).isCloseTo(0.3f, offset(0.0001f));
        }

        @Test
        void emptyText_stillReturnsEmbedding() {
            registerHandler(200, """
                    {"embedding": [0.0, 0.0]}
                    """);

            assertThat(client.embed("")).hasSize(2);
        }

        @Test
        void serverError_returnsEmptyArray() {
            registerHandler(500, "Internal Server Error");

            assertThat(client.embed("some text")).isEmpty();
        }

        @Test
        void unauthorized_returnsEmptyArray() {
            registerHandler(401, "Unauthorized");

            assertThat(client.embed("some text")).isEmpty();
        }
    }

    @Nested
    class WhenNetworkFails {

        @Test
        void unreachableHost_returnsEmptyArray() {
            EmbeddingClient badClient = new EmbeddingClient("http://localhost:1");

            assertThat(badClient.embed("some text")).isEmpty();
        }
    }
}
