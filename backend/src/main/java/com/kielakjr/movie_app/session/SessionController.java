package com.kielakjr.movie_app.session;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(HttpSession httpSession) {
        sessionService.reset(httpSession);
        return ResponseEntity.noContent().build();
    }
}
