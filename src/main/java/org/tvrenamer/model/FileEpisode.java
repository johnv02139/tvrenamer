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
    private static final int NO_INFORMATION = -1;

    private File fileObj;
    private EpisodeStatus status;

    private String filenameShow = "";
    private int filenameSeason = NO_INFORMATION;
    private int filenameEpisode = NO_INFORMATION;
    private String filenameResolution = "";
    private String queryString = "";

    private UserPreferences userPrefs = UserPreferences.getInstance();

    public FileEpisode(String filename) {
        this(new File(filename));
    }

    public FileEpisode(File f) {
        fileObj = f;
        status = EpisodeStatus.UNPARSED;
    }

    public File getFile() {
        return fileObj;
    }

    public void setFile(File fileObj) {
        this.fileObj = fileObj;
    }

    public EpisodeStatus getStatus() {
        return status;
    }

    public void setStatus(EpisodeStatus newStatus) {
        status = newStatus;
    }

    public void setFilenameShow(String filenameShow) {
        this.filenameShow = filenameShow;
        queryString = StringUtils.replacePunctuation(filenameShow).toLowerCase();
    }

    public int getFilenameSeason() {
        return filenameSeason;
    }

    public void setFilenameSeason(int filenameSeason) {
        this.filenameSeason = filenameSeason;
    }

    public int getFilenameEpisode() {
        return filenameEpisode;
    }

    public void setFilenameEpisode(int filenameEpisode) {
        this.filenameEpisode = filenameEpisode;
    }

    public String getFilenameResolution() {
        return filenameResolution;
    }

    public void setFilenameResolution(String filenameResolution) {
        if (filenameResolution == null) {
            this.filenameResolution = "";
        } else {
            this.filenameResolution = filenameResolution;
        }
    }

    public String getQueryString() {
        return queryString;
    }

    private File getDestinationDirectory() {
        String show = ShowStore.mapStringToShow(queryString).getName();
        String destPath = userPrefs.getDestinationDirectory().getAbsolutePath() + File.separatorChar;
        destPath = destPath + StringUtils.sanitiseTitle(show) + File.separatorChar;

        // Defect #50: Only add the 'season #' folder if set, otherwise put files in showname root
        if (StringUtils.isNotBlank(userPrefs.getSeasonPrefix())) {
            destPath = destPath + userPrefs.getSeasonPrefix() + (userPrefs.isSeasonPrefixLeadingZero() && filenameSeason < 9 ? "0" : "") + filenameSeason + File.separatorChar;
        }
        return new File(destPath);
    }

    public String getNewFilename() {
        switch (status) {
            case ADDED: {
                return ADDED_PLACEHOLDER_FILENAME;
            }
            case DOWNLOADED:
            case RENAMED: {
                if (!userPrefs.isRenameEnabled()) {
                    return fileObj.getName();
                }

                String showName = "";
                String seasonNum = "";
                String titleString = "";
                Calendar airDate = Calendar.getInstance();

                try {
                    Show show = ShowStore.mapStringToShow(queryString);
                    showName = show.getName();

                    Season season = show.getSeason(filenameSeason);
                    if (season == null) {
                        seasonNum = String.valueOf(filenameSeason);
                        logger.log(Level.SEVERE, "Season #" + filenameSeason + " not found for show '" + filenameShow + "'");
                    } else {
                        seasonNum = String.valueOf(season.getNumber());

                        try {
                            titleString = season.getTitle(filenameEpisode);
                            Date date = season.getAirDate(filenameEpisode);
                            if (date != null) {
                                airDate.setTime(date);
                            } else {
                                logger.log(Level.WARNING, "Episode air date not found for '" + toString() + "'");
                            }
                        } catch (EpisodeNotFoundException e) {
                            logger.log(Level.SEVERE, "Episode not found for '" + toString() + "'", e);
                        }
                    }
                } catch (ShowNotFoundException e) {
                    showName = filenameShow;
                    logger.log(Level.SEVERE, "Show not found for '" + toString() + "'", e);
                }

                String newFilename = userPrefs.getRenameReplacementString();

                // Ensure that all special characters in the replacement are quoted
                showName = Matcher.quoteReplacement(showName);
                showName = GlobalOverrides.getInstance().getShowName(showName);
                titleString = Matcher.quoteReplacement(titleString);

                // Make whatever modifications are required
                String episodeNumberString = new DecimalFormat("##0").format(filenameEpisode);
                String episodeNumberWithLeadingZeros = new DecimalFormat("#00").format(filenameEpisode);
                String episodeTitleNoSpaces = titleString.replaceAll(" ", ".");
                String seasonNumberWithLeadingZero = new DecimalFormat("00").format(filenameSeason);

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
                                                     filenameResolution);

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

                String resultingFilename = newFilename.concat(StringUtils.getExtension(fileObj.getName()));
                return StringUtils.sanitiseTitle(resultingFilename);
            }
            case UNPARSED:
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
        return "FileEpisode { title:" + filenameShow + ", season:" + filenameSeason + ", episode:" + filenameEpisode
            + ", file:" + fileObj.getName() + " }";
    }
}
