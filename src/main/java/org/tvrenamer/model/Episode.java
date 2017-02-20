package org.tvrenamer.model;

import java.util.Date;

public class Episode {
    private final Season season;
    private final int seasonNum;
    private final int episodeNum;
    private final String title;
    private final Date airDate;

    public Episode(Season season, int seasonNum, int episodeNum, String title, Date airDate) {
        this.season = season;
        this.seasonNum = seasonNum;
        this.episodeNum = episodeNum;
        this.title = title;
        this.airDate = airDate;
    }

    public String getTitle() {
        return this.title;
    }

    public Date getAirDate() {
        return this.airDate;
    }

    @Override
    public String toString() {
        return "Episode [title=" + title + ", airDate=" + airDate + "]";
    }
}
