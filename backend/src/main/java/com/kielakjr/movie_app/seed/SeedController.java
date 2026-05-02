package com.kielakjr.movie_app.seed;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedController {
    private final SeedService seedService;

    @Value("${seed.api-key:}")
    private String seedApiKey;

    @PostMapping("/popular")
    public ResponseEntity<String> seedPopularMovies(
            @RequestHeader(value = "X-Seed-Key", required = false) String key,
            @RequestParam(defaultValue = "5") @Valid @Min(1) @Max(500) int pages) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        int count = seedService.seedPopularMovies(pages);
        return ResponseEntity.ok(count + " movies seeded");
    }

    @PostMapping("/top-rated")
    public ResponseEntity<String> seedTopRatedMovies(
            @RequestHeader(value = "X-Seed-Key", required = false) String key,
            @RequestParam(defaultValue = "5") @Valid @Min(1) @Max(500) int pages) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        int count = seedService.seedTopRatedMovies(pages);
        return ResponseEntity.ok(count + " movies seeded");
    }

    @PostMapping("/all")
    public ResponseEntity<String> seedAllMovies(
            @RequestHeader(value = "X-Seed-Key", required = false) String key,
            @RequestParam(defaultValue = "5") @Valid @Min(1) @Max(500) int pages) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        int popularCount = seedService.seedPopularMovies(pages);
        int topRatedCount = seedService.seedTopRatedMovies(pages);
        return ResponseEntity.ok(popularCount + topRatedCount + " movies seeded (" + popularCount + " popular, " + topRatedCount + " top-rated)");
    }

    private boolean isAuthorized(String key) {
        if (seedApiKey.isBlank()) return true;
        return seedApiKey.equals(key);
    }
}
