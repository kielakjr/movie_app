package com.kielakjr.movie_app.swipe.dto;

public enum SwipeAction {
    LIKE("LIKE"),
    DISLIKE("DISLIKE"),
    SKIP("SKIP");

    private final String action;

    SwipeAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

}
