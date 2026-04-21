package com.kielakjr.movie_app.session;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;

    @GetMapping
    public String test(HttpSession session) {
        Integer count = (Integer) session.getAttribute("count");

        if (count == null) count = 0;
        count++;

        session.setAttribute("count", count);

        return "count = " + count + ", sessionId = " + session.getId();
    }
}
