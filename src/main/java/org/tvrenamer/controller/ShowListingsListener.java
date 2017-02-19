package org.tvrenamer.controller;

import org.tvrenamer.model.Series;

public interface ShowListingsListener {
    void downloadListingsComplete(Series show);

    void downloadListingsFailed(Series show);
}
