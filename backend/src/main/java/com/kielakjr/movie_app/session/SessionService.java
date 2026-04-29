package com.kielakjr.movie_app.session;

import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpSession;

@Service
public class SessionService {
    private static final String KEY = "STATE";

    public SwipeSessionState getState(HttpSession session) {
        var state = (SwipeSessionState) session.getAttribute(KEY);
        if (state == null) {
            state = new SwipeSessionState();
            session.setAttribute(KEY, state);
        }
        return state;
    }

    public void save(HttpSession session, SwipeSessionState state) {
        session.setAttribute(KEY, state);
    }

    public void reset(HttpSession session) {
        session.setAttribute(KEY, new SwipeSessionState());
    }
}
