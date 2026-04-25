package com.kielakjr.movie_app.session;

import lombok.Data;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import com.kielakjr.movie_app.cluster.Cluster;

@Data
public class SwipeSessionState implements Serializable {
    private Set<Long> seenMovieIds = new HashSet<>();
    private Set<Long> dislikedMovieIds = new HashSet<>();
    private Set<Long> likedMovieIds = new HashSet<>();
    private List<Cluster> clusters = new ArrayList<>();
    private int likesCount = 0;
}
