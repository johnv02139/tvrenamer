package org.tvrenamer.controller;

import org.tvrenamer.model.Series;

public interface EpisodeListListener {
    void downloadListingsComplete(Series show);

    void downloadListingsFailed(Series show);
}
