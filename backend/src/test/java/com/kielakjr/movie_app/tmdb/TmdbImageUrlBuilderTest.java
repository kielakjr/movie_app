package com.kielakjr.movie_app.tmdb;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TmdbImageUrlBuilderTest {

    @Nested
    class Construction {

        @Test
        void blankPosterBaseUrl_throwsIllegalState() {
            assertThrows(IllegalStateException.class,
                    () -> new TmdbImageUrlBuilder("", "https://valid.url"));
        }

        @Test
        void blankBackdropBaseUrl_throwsIllegalState() {
            assertThrows(IllegalStateException.class,
                    () -> new TmdbImageUrlBuilder("https://valid.url", "  "));
        }
    }

    @Nested
    class PosterUrl {

        final TmdbImageUrlBuilder builder =
                new TmdbImageUrlBuilder("https://image.tmdb.org/t/p/w500", "https://image.tmdb.org/t/p/original");

        @Test
        void withLeadingSlash_buildsCorrectUrl() {
            assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", builder.posterUrl("/abc.jpg"));
        }

        @Test
        void withoutLeadingSlash_buildsCorrectUrl() {
            assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", builder.posterUrl("abc.jpg"));
        }

        @Test
        void absoluteUrl_returnedAsIs() {
            String url = "https://example.com/img.jpg";
            assertEquals(url, builder.posterUrl(url));
        }

        @Test
        void nullPath_returnsNull() {
            assertNull(builder.posterUrl(null));
        }

        @Test
        void emptyPath_returnsNull() {
            assertNull(builder.posterUrl(""));
        }

        @Test
        void blankPath_returnsNull() {
            assertNull(builder.posterUrl("   "));
        }

        @Test
        void baseUrlWithTrailingSlash_isTrimmed() {
            TmdbImageUrlBuilder slashy = new TmdbImageUrlBuilder("https://img/", "https://img/");
            assertEquals("https://img/abc.jpg", slashy.posterUrl("/abc.jpg"));
        }
    }

    @Nested
    class BackdropUrl {

        final TmdbImageUrlBuilder builder =
                new TmdbImageUrlBuilder("https://image.tmdb.org/t/p/w500", "https://image.tmdb.org/t/p/original");

        @Test
        void withLeadingSlash_buildsCorrectUrl() {
            assertEquals("https://image.tmdb.org/t/p/original/backdrop.jpg", builder.backdropUrl("/backdrop.jpg"));
        }

        @Test
        void nullPath_returnsNull() {
            assertNull(builder.backdropUrl(null));
        }

        @Test
        void emptyPath_returnsNull() {
            assertNull(builder.backdropUrl(""));
        }
    }
}
