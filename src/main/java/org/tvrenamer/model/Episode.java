package org.tvrenamer.model;

import java.time.LocalDate;

public class Episode {
    private static final int NO_SEASON = -1;
    private static final int NO_EPISODE = -1;

    private final int seasonNum;
    private final int episodeNum;
    private final String title;
    private final LocalDate airDate;
    private Season season = null;

    public static class Builder {
        private Season season = null;
        private int seasonNum = NO_SEASON;
        private int episodeNum = NO_EPISODE;
        private String title = null;
        private LocalDate airDate = null;

        public Builder() {
        }

        public Builder season(Season val) {
            season = val;
            return this;
        }

        public Builder seasonNum(int val) {
            seasonNum = val;
            return this;
        }

        public Builder episodeNum(int val) {
            episodeNum = val;
            return this;
        }

        public Builder title(String val) {
            title = val;
            return this;
        }

        public Builder airDate(LocalDate val) {
            airDate = val;
            return this;
        }

        public Episode build() {
            return new Episode(this);
        }
    }

    public Episode(Builder builder) {
        season = builder.season;
        seasonNum = builder.seasonNum;
        episodeNum = builder.episodeNum;
        title = builder.title;
        airDate = builder.airDate;
    }

    public void setSeason(Season s) {
        this.season = s;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getAirDate() {
        return airDate;
    }

    @Override
    public String toString() {
        return "Episode [title=" + title + ", airDate=" + airDate + "]";
    }
}
