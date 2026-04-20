package com.kielakjr.movie_app.tmdb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TmdbImageUrlBuilder {

    private final String posterBaseUrl;
    private final String backdropBaseUrl;

    public TmdbImageUrlBuilder(
            @Value("${tmdb.images.poster-base-url}") String posterBaseUrl,
            @Value("${tmdb.images.backdrop-base-url}") String backdropBaseUrl
    ) {
        if (posterBaseUrl == null || posterBaseUrl.isBlank())
            throw new IllegalStateException("tmdb.images.poster-base-url must be configured");
        if (backdropBaseUrl == null || backdropBaseUrl.isBlank())
            throw new IllegalStateException("tmdb.images.backdrop-base-url must be configured");
        this.posterBaseUrl = trimTrailingSlash(posterBaseUrl);
        this.backdropBaseUrl = trimTrailingSlash(backdropBaseUrl);
    }

    public String posterUrl(String posterPath) {
        return build(posterBaseUrl, posterPath);
    }

    public String backdropUrl(String backdropPath) {
        return build(backdropBaseUrl, backdropPath);
    }

    private static String build(String base, String path) {
        if (path == null || path.isBlank()) return null;

        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        if (path.startsWith("/")) return base + path;
        return base + "/" + path;
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return null;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
