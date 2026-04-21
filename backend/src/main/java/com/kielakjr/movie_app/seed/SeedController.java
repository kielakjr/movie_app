package com.kielakjr.movie_app.seed;

import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedController {
    private final SeedService seedService;

    @PostMapping("/popular")
    public ResponseEntity<String> seedPopularMovies(@RequestParam(defaultValue = "5") @Valid @Min(1) @Max(500) int pages) {
        int count = seedService.seedPopularMovies(pages);
        return ResponseEntity.ok().body(count + " movies seeded");
    }
}
