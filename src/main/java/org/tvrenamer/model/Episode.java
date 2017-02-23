package org.tvrenamer.model;

import org.tvrenamer.controller.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

public class Episode {

    private static Logger logger = Logger.getLogger(Episode.class.getName());

    private static final String EPISODE_DATE_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(EPISODE_DATE_FORMAT);

    private final String seasonNumString;
    private final String episodeNumString;
    private final String dvdEpisodeNumString;
    private final String title;
    private final LocalDate airDate;

    public static class Builder {
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

        public Builder airDate(String val) {
            if (StringUtils.isBlank(val)) {
                airDate = LocalDate.now();
            } else {
                try {
                    airDate = LocalDate.parse(val, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    // While a null or empty string is taken to mean "now",
                    // a badly formatted string is an error and will not
                    // be translated into any date.
                    logger.warning("could not parse as date: " + val);
                    airDate = null;
                }
            }
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
        return "Episode S" + seasonNumString
            + "E" + episodeNumString
            + "[title=" + title
            + ", airDate=" + airDate + "]";
    }
}
