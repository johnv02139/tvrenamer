package org.tvrenamer.controller;

import org.tvrenamer.model.FileEpisode;

public interface EpisodeInformationListener {
    void onEpisodeUpdate(final FileEpisode episode);
}
