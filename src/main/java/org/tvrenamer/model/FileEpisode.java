package org.tvrenamer.model;

import org.tvrenamer.controller.util.StringUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class FileEpisode {
    private static Logger logger = Logger.getLogger(FileEpisode.class.getName());

    private static final String ADDED_PLACEHOLDER_FILENAME = "Downloading ...";
    private static final String BROKEN_PLACEHOLDER_FILENAME = "Unable to download show information";
    private final String filenameShow;
    private final String queryString;
    private final int seasonNumber;
    private final int episodeNumber;
    private final String episodeResolution;
    private File file;

    private UserPreferences userPrefs = UserPreferences.getInstance();
    private EpisodeInfo status;

    public FileEpisode(String name, int season, int episode, String resolution, File f) {
        filenameShow = name;
        queryString = StringUtils.replacePunctuation(name).toLowerCase();
        seasonNumber = season;
        episodeNumber = episode;
        episodeResolution = resolution;
        file = f;
        status = EpisodeInfo.ADDED;
    }

    public String getQueryString() {
        return queryString;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public String getEpisodeResolution() {
        return episodeResolution;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File f) {
        file = f;
    }

    public EpisodeInfo getStatus() {
        return this.status;
    }

    public void setStatus(EpisodeInfo newStatus) {
        this.status = newStatus;
    }

    private File getDestinationDirectory() {
        String show = ShowStore.getShow(queryString).getName();
        String destPath = userPrefs.getDestinationDirectory().getAbsolutePath() + File.separatorChar;
        destPath = destPath + StringUtils.sanitiseTitle(show) + File.separatorChar;

        // Defect #50: Only add the 'season #' folder if set, otherwise put files in showname root
        if (StringUtils.isNotBlank(userPrefs.getSeasonPrefix())) {
            destPath = destPath + userPrefs.getSeasonPrefix() + (userPrefs.isSeasonPrefixLeadingZero() && this.seasonNumber < 9 ? "0" : "") + this.seasonNumber + File.separatorChar;
        }
        return new File(destPath);
    }

    public String getNewFilename() {
        switch (this.status) {
            case ADDED: {
                return ADDED_PLACEHOLDER_FILENAME;
            }
            case DOWNLOADED:
            case RENAMED: {
                String showName = "";
                String seasonNum = "";
                String titleString = "";
                Calendar airDate = Calendar.getInstance();

                try {
                    Show show = ShowStore.getShow(queryString);
                    showName = show.getName();

                    Season season = show.getSeason(this.seasonNumber);
                    if (season == null) {
                        seasonNum = String.valueOf(this.seasonNumber);
                        logger.log(Level.SEVERE, "Season #" + this.seasonNumber + " not found for show '" + filenameShow + "'");
                    } else {
                        seasonNum = String.valueOf(season.getNumber());

                        try {
                            titleString = season.getTitle(this.episodeNumber);
                            Date date = season.getAirDate(this.episodeNumber);
                            if (date != null) {
                                airDate.setTime(date);
                            } else {
                                logger.log(Level.WARNING, "Episode air date not found for '" + this.toString() + "'");
                            }
                        } catch (EpisodeNotFoundException e) {
                            logger.log(Level.SEVERE, "Episode not found for '" + this.toString() + "'", e);
                        }
                    }
                } catch (ShowNotFoundException e) {
                    showName = filenameShow;
                    logger.log(Level.SEVERE, "Show not found for '" + this.toString() + "'", e);
                }

                String newFilename = userPrefs.getRenameReplacementString();

                // Ensure that all special characters in the replacement are quoted
                showName = Matcher.quoteReplacement(showName);
                showName = GlobalOverrides.getInstance().getShowName(showName);
                titleString = Matcher.quoteReplacement(titleString);

                // Make whatever modifications are required
                String episodeNumberString = new DecimalFormat("##0").format(this.episodeNumber);
                String episodeNumberWithLeadingZeros = new DecimalFormat("#00").format(this.episodeNumber);
                String episodeTitleNoSpaces = titleString.replaceAll(" ", ".");
                String seasonNumberWithLeadingZero = new DecimalFormat("00").format(this.seasonNumber);

                newFilename = newFilename.replaceAll(ReplacementToken.SHOW_NAME.getToken(), showName);
                newFilename = newFilename.replaceAll(ReplacementToken.SEASON_NUM.getToken(), seasonNum);
                newFilename = newFilename.replaceAll(ReplacementToken.SEASON_NUM_LEADING_ZERO.getToken(),
                                                     seasonNumberWithLeadingZero);
                newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_NUM.getToken(), episodeNumberString);
                newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_NUM_LEADING_ZERO.getToken(),
                                                     episodeNumberWithLeadingZeros);
                newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_TITLE.getToken(), titleString);
                newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_TITLE_NO_SPACES.getToken(),
                                                     episodeTitleNoSpaces);
                newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_RESOLUTION.getToken(),
                                                     episodeResolution);

                // Date and times
                newFilename = newFilename
                    .replaceAll(ReplacementToken.DATE_DAY_NUM.getToken(), formatDate(airDate, "d"));
                newFilename = newFilename.replaceAll(ReplacementToken.DATE_DAY_NUMLZ.getToken(),
                                                     formatDate(airDate, "dd"));
                newFilename = newFilename.replaceAll(ReplacementToken.DATE_MONTH_NUM.getToken(),
                                                     formatDate(airDate, "M"));
                newFilename = newFilename.replaceAll(ReplacementToken.DATE_MONTH_NUMLZ.getToken(),
                                                     formatDate(airDate, "MM"));
                newFilename = newFilename.replaceAll(ReplacementToken.DATE_YEAR_FULL.getToken(),
                                                     formatDate(airDate, "yyyy"));
                newFilename = newFilename.replaceAll(ReplacementToken.DATE_YEAR_MIN.getToken(),
                                                     formatDate(airDate, "yy"));

                String resultingFilename = newFilename.concat(StringUtils.getExtension(file.getName()));
                return StringUtils.sanitiseTitle(resultingFilename);
            }
            case BROKEN:
            default:
                return BROKEN_PLACEHOLDER_FILENAME;
        }
    }

    private String formatDate(Calendar cal, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(cal.getTime());
    }

    /**
     * @return the new full file path (for table display) using {@link #getNewFilename()} and the destination directory
     */
    public String getNewFilePath() {
        String filename = getNewFilename();

        if (userPrefs.isMoveEnabled()) {
            return getDestinationDirectory().getAbsolutePath().concat(File.separator).concat(filename);
        }
        return filename;
    }

    @Override
    public String toString() {
        return "FileEpisode { title:" + filenameShow + ", season:" + seasonNumber + ", episode:" + episodeNumber
            + ", file:" + file.getName() + " }";
    }
}
