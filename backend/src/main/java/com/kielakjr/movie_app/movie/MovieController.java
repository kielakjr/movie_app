package com.kielakjr.movie_app.movie;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.web.PagedModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import com.kielakjr.movie_app.movie.dto.MovieResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @GetMapping
    public ResponseEntity<PagedModel<MovieResponse>> getAllMovies(
        @RequestParam(value = "page", defaultValue = "0") @Valid @Min(0) @Max(Integer.MAX_VALUE) int page,
        @RequestParam(value = "size", defaultValue = "20") @Valid @Min(1) @Max(100) int size
    ) {
        var pageable = PageRequest.of(page, size);
        var moviePage = movieService.getAllMovies(pageable);
        var pagedModel = new PagedModel<>(moviePage);
        return ResponseEntity.ok(pagedModel);
    }
}
