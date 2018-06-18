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

    private final String seasonNumber;
    private final String episodeNumber;
    private final String episodeName;
    private final LocalDate firstAired;
    private final String overview;
    private final String productionCode;
    private final String language;
    private final String id;
    private final String seriesid;
    private final String seasonid;
    private final Integer lastupdated;
    private final String dvdSeason;
    private final String dvdEpisodeNumber;

    public static class Builder {
        private String seasonNumber;
        private String episodeNumber;
        private String episodeName = null;
        private LocalDate firstAired = null;
        private String overview = null;
        private String productionCode = null;
        private String language = null;
        private String id = null;
        private String seriesid = null;
        private String seasonid = null;
        private Integer lastupdated = null;
        private String dvdSeason;
        private String dvdEpisodeNumber;

        public Builder() {
        }

        public Builder seasonNum(String val) {
            seasonNumber = val;
            return this;
        }

        public Builder episodeNum(String val) {
            episodeNumber = val;
            return this;
        }

        public Builder title(String val) {
            episodeName = val;
            return this;
        }

        public Builder airDate(String val) {
            if (StringUtils.isBlank(val)) {
                firstAired = LocalDate.now();
            } else {
                try {
                    firstAired = LocalDate.parse(val, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    // While a null or empty string is taken to mean "now",
                    // a badly formatted string is an error and will not
                    // be translated into any date.
                    logger.warning("could not parse as date: " + val);
                    firstAired = null;
                }
            }
            return this;
        }

        public Builder overview(String val) {
            overview = val;
            return this;
        }

        public Builder productionCode(String val) {
            productionCode = val;
            return this;
        }

        public Builder language(String val) {
            language = val;
            return this;
        }

        public Builder id(String val) {
            id = val;
            return this;
        }

        public Builder seriesid(String val) {
            seriesid = val;
            return this;
        }

        public Builder seasonid(String val) {
            seasonid = val;
            return this;
        }

        public Builder lastupdated(String val) {
            lastupdated = StringUtils.stringToInt(val);
            return this;
        }

        public Builder dvdSeason(String val) {
            dvdSeason = val;
            return this;
        }

        public Builder dvdEpisodeNumber(String val) {
            dvdEpisodeNumber = val;
            return this;
        }

        public Episode build() {
            return new Episode(this);
        }
    }

    public Episode(Builder builder) {
        seasonNumber = builder.seasonNumber;
        episodeNumber = builder.episodeNumber;
        episodeName = builder.episodeName;
        firstAired = builder.firstAired;
        overview = builder.overview;
        productionCode = builder.productionCode;
        language = builder.language;
        id = builder.id;
        seriesid = builder.seriesid;
        seasonid = builder.seasonid;
        lastupdated = builder.lastupdated;
        dvdSeason = builder.dvdSeason;
        dvdEpisodeNumber = builder.dvdEpisodeNumber;

        logger.finer("[S" + seasonNumber + "E" + episodeNumber + "] " + episodeName);
    }

    public String getSeasonNumberText() {
        return seasonNumber;
    }

    public String getEpisodeNumberText() {
        return episodeNumber;
    }

    public String getTitle() {
        return episodeName;
    }

    public LocalDate getAirDate() {
        return firstAired;
    }

    @Override
    public String toString() {
        return "Episode S" + seasonNumber
            + "E" + episodeNumber
            + "[episodeName=" + episodeName
            + ", firstAired=" + firstAired + "]";
    }
}
