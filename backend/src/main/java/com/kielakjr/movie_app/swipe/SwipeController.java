package com.kielakjr.movie_app.swipe;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import com.kielakjr.movie_app.swipe.dto.SwipeRequest;

@RestController
@RequestMapping("/api/swipe")
@RequiredArgsConstructor
public class SwipeController {
    private final SwipeService swipeService;

    @PostMapping
    public ResponseEntity<Void> swipe(
        @RequestBody @Valid SwipeRequest request,
        HttpSession httpSession
    ) {
        swipeService.swipe(request, httpSession);
        return ResponseEntity.ok().build();
    }
}
