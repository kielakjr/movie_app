package com.kielakjr.movie_app.session;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
public class SwipeSessionState implements Serializable {
    private Set<Long> seenMovieIds;
    private Set<Long> dislikedMovieIds;

    private List<Float> userEmbedding;
    private int likesCount;
}
