package org.tvrenamer.controller;

import org.tvrenamer.model.Series;

public interface SeriesLookupListener {
    void downloadComplete(Series show);

    void downloadFailed(Series show);
}
