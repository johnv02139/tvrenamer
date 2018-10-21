package org.tvrenamer.controller;

import org.tvrenamer.model.Show;

import java.util.List;

public interface ShowInformationListener {
    void downloadComplete(List<Show> shows);
}
