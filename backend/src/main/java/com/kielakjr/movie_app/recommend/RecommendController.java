package com.kielakjr.movie_app.recommend;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.kielakjr.movie_app.recommend.dto.RecommendMovieResponse;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.servlet.http.HttpSession;
import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {
    private final RecommendService recommendService;

    @GetMapping
    public List<RecommendMovieResponse> getRecommendedMovies(
        @RequestParam(value = "limit", defaultValue = "10") @Valid @Min(1) @Max(100) int limit,
        HttpSession session
    ) {
        return recommendService.getRecommendedMovies(session, limit);
    }
}
