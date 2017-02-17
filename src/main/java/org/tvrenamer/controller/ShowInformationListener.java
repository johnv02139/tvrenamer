package org.tvrenamer.controller;

import org.tvrenamer.model.Show;

public interface ShowInformationListener {
    void downloadComplete(Show show);

    void downloadFailed(Show show);
}
