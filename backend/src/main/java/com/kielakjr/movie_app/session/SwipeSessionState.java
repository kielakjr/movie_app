package com.kielakjr.movie_app.session;

import lombok.Data;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class SwipeSessionState implements Serializable {
    private Set<Long> seenMovieIds = new HashSet<>();
    private Set<Long> dislikedMovieIds = new HashSet<>();
    private Set<Long> likedMovieIds = new HashSet<>();
    private List<Float> userEmbedding;
    private int likesCount;
}
