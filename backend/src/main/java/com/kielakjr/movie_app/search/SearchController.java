package com.kielakjr.movie_app.search;

import lombok.RequiredArgsConstructor;
import com.kielakjr.movie_app.movie.dto.MovieResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping("/similar")
    public ResponseEntity<List<MovieResponse>> searchSimilar(
        @RequestParam(value = "query", required = true) @Valid @NotBlank @Size(min = 2, max = 300) String query,
        @RequestParam(value = "limit", defaultValue = "10") @Valid @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(searchService.searchSimilar(query, limit));
    }
}
