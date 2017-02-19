package org.tvrenamer.controller;

import org.tvrenamer.model.Series;

public interface ShowInformationListener {
    void downloadComplete(Series show);

    void downloadFailed(Series show);
}
