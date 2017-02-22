package org.tvrenamer.model;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.except.EpisodeNotFoundException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Season {
    private final Series series;
    private final String seasonNumber;
    private final Map<Integer, Episode> episodes;

    public Season(Series series, String seasonNumber) {
        this.series = series;
        this.seasonNumber = seasonNumber;
        episodes = new HashMap<>();
    }

    public String getNumber() {
        return seasonNumber;
    }

    public void addEpisode(Episode episode) {
        String epNumText = episode.getEpisodeNumberText();
        Integer epNum = StringUtils.stringToInt(epNumText);
        if (epNum != null) {
            episodes.put(epNum, episode);
        }
    }

    public Episode getEpisode(int epNum) {
        return episodes.get(epNum);
    }

    public String getTitle(int epNum) {
        Episode e = episodes.get(epNum);
        if (e == null) {
            throw new EpisodeNotFoundException("Episode #" + epNum + " not found for season #" + seasonNumber
                                               + " of series " + series.getName());
        }
        return e.getTitle();
    }

    public LocalDate getAirDate(int epNum) {
        Episode e = episodes.get(epNum);
        if (e == null) {
            throw new EpisodeNotFoundException("Episode #" + epNum + " not found for season #" + seasonNumber
                                               + " of series " + series.getName());
        }
        return e.getAirDate();
    }

    @Override
    public String toString() {
        return "Season [num=" + seasonNumber + ", " + episodes.size() + " episodes]";
    }

    public String toLongString() {
        return "Season [num=" + seasonNumber + ", episodes=" + episodes + "]";
    }
}
