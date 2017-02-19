package org.tvrenamer.model;

import org.tvrenamer.model.except.EpisodeNotFoundException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Season {
    private final Series series;
    private final int num;
    private final Map<Integer, Episode> episodes;

    public Season(Series series, int num) {
        this.series = series;
        this.num = num;
        episodes = new HashMap<>();
    }

    public int getNumber() {
        return num;
    }

    public void addEpisode(int epNum, String title, LocalDate airDate) {
        episodes.put(epNum, new Episode(this, num, epNum, title, airDate));
    }

    public String getTitle(int epNum) {
        Episode e = episodes.get(epNum);
        if (e == null) {
            throw new EpisodeNotFoundException("Episode #" + epNum + " not found for season #" + this.num
                                               + " of series " + series.getName());
        }
        return e.getTitle();
    }

    public LocalDate getAirDate(int epNum) {
        Episode e = episodes.get(epNum);
        if (e == null) {
            throw new EpisodeNotFoundException("Episode #" + epNum + " not found for season #" + this.num
                                               + " of series " + series.getName());
        }
        return e.getAirDate();
    }

    @Override
    public String toString() {
        return "Season [num=" + num + ", " + episodes.size() + " episodes]";
    }

    public String toLongString() {
        return "Season [num=" + num + ", episodes=" + episodes + "]";
    }
}
