package com.kielakjr.movie_app.tmdb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TmdbImageUrlBuilderTest {

    @Test
    void shouldBuildPosterUrlWithLeadingSlash() {
        TmdbImageUrlBuilder builder =
                new TmdbImageUrlBuilder("https://image.tmdb.org/t/p/w500", "https://image.tmdb.org/t/p/original");

        String result = builder.posterUrl("/abc.jpg");

        assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", result);
    }

    @Test
    void shouldBuildPosterUrlWithoutLeadingSlash() {
        TmdbImageUrlBuilder builder =
                new TmdbImageUrlBuilder("https://image.tmdb.org/t/p/w500", "https://image.tmdb.org/t/p/original");

        String result = builder.posterUrl("abc.jpg");

        assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", result);
    }

    @Test
    void shouldReturnNullForBlankPath() {
        TmdbImageUrlBuilder builder =
                new TmdbImageUrlBuilder("base", "base");

        assertNull(builder.posterUrl(null));
        assertNull(builder.posterUrl(""));
    }

    @Test
    void shouldReturnFullUrlIfAlreadyAbsolute() {
        TmdbImageUrlBuilder builder =
                new TmdbImageUrlBuilder("base", "base");

        String url = "https://example.com/img.jpg";

        assertEquals(url, builder.posterUrl(url));
    }

    @Test
    void shouldTrimTrailingSlashFromBaseUrl() {
        TmdbImageUrlBuilder builder =
                new TmdbImageUrlBuilder("https://img/", "https://img/");

        String result = builder.posterUrl("/abc.jpg");

        assertEquals("https://img/abc.jpg", result);
    }
}
