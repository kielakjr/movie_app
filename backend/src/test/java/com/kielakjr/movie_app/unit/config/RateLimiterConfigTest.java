package com.kielakjr.movie_app.unit.config;

import com.kielakjr.movie_app.config.RateLimiterConfig;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterConfigTest {

    private final RateLimiterConfig config = new RateLimiterConfig();

    @Test
    void tmdbBucket_startsAtFullCapacityOf20() {
        Bucket bucket = config.tmdbBucket();

        assertThat(bucket.getAvailableTokens()).isEqualTo(20);
    }

    @Test
    void tmdbBucket_allowsConsumingUpTo20TokensAtOnce() {
        Bucket bucket = config.tmdbBucket();

        assertThat(bucket.tryConsume(20)).isTrue();
        assertThat(bucket.getAvailableTokens()).isZero();
    }

    @Test
    void tmdbBucket_rejectsConsumingMoreThan20Tokens() {
        Bucket bucket = config.tmdbBucket();

        assertThat(bucket.tryConsume(21)).isFalse();
    }
}
