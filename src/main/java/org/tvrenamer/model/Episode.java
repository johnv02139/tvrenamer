package org.tvrenamer.model;

import java.time.LocalDate;
import java.util.logging.Logger;

public class Episode {

    private static Logger logger = Logger.getLogger(Episode.class.getName());

    private final String seasonNumString;
    private final String episodeNumString;
    private final String dvdEpisodeNumString;
    private final String title;
    private final LocalDate airDate;
    private Season season = null;

    public static class Builder {
        private Season season = null;
        private String seasonNumString;
        private String episodeNumString;
        private String dvdEpisodeNumString;
        private String title = null;
        private LocalDate airDate = null;

        public Builder() {
        }

        public Builder seasonNum(String val) {
            seasonNumString = val;
            return this;
        }

        public Builder episodeNum(String val) {
            episodeNumString = val;
            return this;
        }

        public Builder dvdEpisodeNum(String val) {
            dvdEpisodeNumString = val;
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
        seasonNumString = builder.seasonNumString;
        episodeNumString = builder.episodeNumString;
        dvdEpisodeNumString = builder.dvdEpisodeNumString;
        title = builder.title;
        airDate = builder.airDate;

        logger.finer("[S" + seasonNumString + "E" + episodeNumString + "] " + title);
    }

    public void setSeason(Season s) {
        this.season = s;
    }

    public String getSeasonNumberText() {
        return seasonNumString;
    }

    public String getEpisodeNumberText() {
        return episodeNumString;
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
