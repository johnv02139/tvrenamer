package org.tvrenamer.controller;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.Episode;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.GlobalOverrides;
import org.tvrenamer.model.ReplacementToken;
import org.tvrenamer.model.Season;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.UserPreferences;

import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class NameFormatter {

    private static Logger logger = Logger.getLogger(NameFormatter.class.getName());

    private static final String DATEDAY_NUM = ReplacementToken.DATE_DAY_NUM.getToken();
    private static final String DATEDAY_NLZ = ReplacementToken.DATE_DAY_NUMLZ.getToken();
    private static final String DATEMON_NUM = ReplacementToken.DATE_MONTH_NUM.getToken();
    private static final String DATEMON_NLZ = ReplacementToken.DATE_MONTH_NUMLZ.getToken();
    private static final String DATE_YR_FUL = ReplacementToken.DATE_YEAR_FULL.getToken();
    private static final String DATE_YR_MIN = ReplacementToken.DATE_YEAR_MIN.getToken();
    private static final String ERESOLUTION = ReplacementToken.EPISODE_RESOLUTION.getToken();
    private static final String EPISODE_NUM = ReplacementToken.EPISODE_NUM.getToken();
    private static final String ENUM_LEADZR = ReplacementToken.EPISODE_NUM_LEADING_ZERO.getToken();
    private static final String EPISD_TITLE = ReplacementToken.EPISODE_TITLE.getToken();
    private static final String EP_TIT_NOSP = ReplacementToken.EPISODE_TITLE_NO_SPACES.getToken();
    private static final String SEAS_NUMBER = ReplacementToken.SEASON_NUM.getToken();
    private static final String SNUM_LEADZR = ReplacementToken.SEASON_NUM_LEADING_ZERO.getToken();
    private static final String SERIES_NAME = ReplacementToken.SERIES_NAME.getToken();

    public static final ThreadLocal<DecimalFormat> TWO_DIGITS =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("00");
            }
        };

    public static final ThreadLocal<DecimalFormat> DIGITS =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("##0");
            }
        };

    public static final ThreadLocal<DecimalFormat> TWO_OR_THREE =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("#00");
            }
        };

    private final Series series;
    private final Season season;
    private final Episode episode;
    private final int seasonNum;
    private final int episodeNum;
    private final String episodeResolution;
    private final String fileSuffix;

    // This class actually figures out the proposed new name for the file, so we need
    // a link to the user preferences to know how the user wants the file renamed.
    private final UserPreferences userPrefs = UserPreferences.getInstance();
    private final GlobalOverrides overrides = GlobalOverrides.getInstance();

    private String seasonSubdir() {
        return userPrefs.getSeasonPrefix()
            + String.format((userPrefs.isSeasonPrefixLeadingZero() ? "%02d" : "%d"),
                            seasonNum);
    }

    private String addDestinationDirectory(String filename) {
        String dirname = (series == null) ? "" : series.getDirName();
        File destPath = new File(userPrefs.getDestinationDirectory(), dirname);

        // Defect #50: Only add the 'season #' folder if set, otherwise put files in showname root
        if (StringUtils.isNotBlank(userPrefs.getSeasonPrefix())) {
            destPath = new File(destPath, seasonSubdir());
        }
        File destFile = new File(destPath, filename);
        return destFile.getAbsolutePath();
    }

    private String replaceDate(String orig, String match, LocalDate date, String format) {
        if (date == null) {
            // TODO: any kind of logging here?
            return orig.replaceAll(match, "");
        } else {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(format);
            return orig.replaceAll(match, dateFormat.format(date));
        }
    }

    private String transformedFilename(String officialSeriesName,
                                       String titleString,
                                       LocalDate airDate)
    {
        String nf = userPrefs.getRenameReplacementString();

        // Make whatever modifications are required
        nf = nf.replaceAll(SERIES_NAME, officialSeriesName);
        nf = nf.replaceAll(SEAS_NUMBER, String.valueOf(seasonNum));
        nf = nf.replaceAll(SNUM_LEADZR, TWO_DIGITS.get().format(seasonNum));
        nf = nf.replaceAll(EPISODE_NUM, DIGITS.get().format(episodeNum));
        nf = nf.replaceAll(ENUM_LEADZR, TWO_OR_THREE.get().format(episodeNum));
        nf = nf.replaceAll(EPISD_TITLE, Matcher.quoteReplacement(titleString));
        nf = nf.replaceAll(EP_TIT_NOSP, Matcher.quoteReplacement(StringUtils.makeDotTitle(titleString)));
        nf = nf.replaceAll(ERESOLUTION, episodeResolution);

        // Date and times
        nf = replaceDate(nf, DATEDAY_NUM, airDate, "d");
        nf = replaceDate(nf, DATEDAY_NLZ, airDate, "dd");
        nf = replaceDate(nf, DATEMON_NUM, airDate, "M");
        nf = replaceDate(nf, DATEMON_NLZ, airDate, "MM");
        nf = replaceDate(nf, DATE_YR_FUL, airDate, "yyyy");
        nf = replaceDate(nf, DATE_YR_MIN, airDate, "yy");

        nf = StringUtils.sanitiseTitle(nf);

        return nf;
    }

    private String getOfficialSeriesName() {
        String officialSeriesName = series.getName();
        // Ensure that all special characters in the replacement are quoted
        officialSeriesName = Matcher.quoteReplacement(officialSeriesName);
        officialSeriesName = overrides.applyTitleOverride(officialSeriesName);

        return officialSeriesName;
    }

    private String getTitleString() {
        if (episode == null) {
            return "";
        }
        return episode.getTitle();
    }

    private LocalDate getAirDate() {
        if (episode == null) {
            return null;
        }
        LocalDate airDate = episode.getAirDate();
        if (airDate == null) {
            logger.warning("Episode air date not found for '"
                           + toString() + "'");
        }
        return airDate;
    }

    public String getNewBasename() {
        return transformedFilename(getOfficialSeriesName(),
                                   getTitleString(),
                                   getAirDate());
    }

    public String getProposedFilename(String newFilename) {
        if (userPrefs.isMoveEnabled()) {
            return addDestinationDirectory(newFilename);
        } else {
            return newFilename;
        }
    }

    public String getProposedFilename() {
        return getProposedFilename(getNewBasename() + fileSuffix);
    }

    public NameFormatter(FileEpisode ep) {
        series = ep.getSeries();
        if (series == null) {
            throw new IllegalArgumentException("series must not be null");
        }
        season = ep.getSeason();
        if (season == null) {
            throw new IllegalArgumentException("season must not be null");
        }

        seasonNum = StringUtils.stringToInt(ep.getFilenameSeason());
        episodeNum = StringUtils.stringToInt(ep.getFilenameEpisode());

        // The resolution comes directly from the original filename, and therefore
        // cannot require "sanitising".
        episodeResolution = Matcher.quoteReplacement(ep.getFilenameResolution());

        episode = season.getEpisode(episodeNum);
        if (episode == null) {
            logger.warning("no episode found for " + series.getName() + ", season "
                           + seasonNum + ", episode " + episodeNum);
        }

        fileSuffix = ep.getFilenameSuffix();
    }

    @Override
    public String toString() {
        return "NameFormatter { series:"
            + series.getName()
            + ", season:"
            + seasonNum
            + ", episode:"
            + episodeNum
            + " }";
    }
}
