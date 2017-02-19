// FileEpisode - represents a file on disk which is presumed to contain a single
//   episode of a TV show.
//
// This is a very mutable class.  It is initially created with just a filename,
// and then information comes streaming in.
//

package org.tvrenamer.model;

import org.eclipse.swt.widgets.TableItem;
import org.tvrenamer.controller.EpisodeInformationListener;
import org.tvrenamer.controller.ListingsLookup;
import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.ShowListingsListener;
import org.tvrenamer.controller.TVRenamer;
import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.except.EpisodeNotFoundException;

import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class FileEpisode implements ShowInformationListener, ShowListingsListener {

    private static Logger logger = Logger.getLogger(FileEpisode.class.getName());

    private enum EpisodeStatus {
        UNPARSED,
        PARSED,
        BAD_PARSE,

        GOT_SHOW,
        DOWNLOADED,
        UNFOUND,
        NO_LISTINGS,

        MOVING,
        RENAMED,
        FAIL_TO_MOVE
        // Should we have a status for if the file does not (any longer) exist?
    }

    // The EpisodeStatus enum refers to aspects of how we've parsed the filename
    // and how we've processed the file.  This separate enum refers to whether or
    // not the FileEpisode is showing in the UI.
    private enum EpisodeUIStatus {
        NEW,
        ADDED,
        REMOVED,
        ERROR
    }

    public static final int NO_SEASON = -1;
    public static final int NO_EPISODE = -1;

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
    private static final String SERIES_NAME = ReplacementToken.SHOW_NAME.getToken();

    // Although we also store the file object that clearly gives us all the information
    // about the file's name, store this explicitly to know what we were given.  Note,
    // even after the file is moved, this value remains the same.  Note also this could
    // be a non-normalized, relative pathstring, for example.
    private final String originalFilename;

    // "filename" instance vars -- these are the results of parsing the filename.
    // The "filenameShow" is the precise string from the filename, that we think
    // corresponds to the name of the TV series.  We use these data to look up
    // information about the show, and the information we find may differ from
    // what the filename actually represents.
    private final String filenameSuffix;
    private String filenameShow;
    private int filenameSeason;
    private int filenameEpisode;
    private String filenameResolution = "";

    private String fileBasename = null;

    // The "fileObj" is the java.io.File object representing the file that this
    // object is concerned with.  Note that it is non-final, as the FileMover
    // class may change the file associated with this episode.  After all, the
    // whole point here is to rename files.  Nevertheless, not sure that detail
    // makes sense, particularly given that the three "filename" instance variables,
    // above, are final.  After a move, the variables are not necessarily coordinated
    // with each other.
    //
    // A better approach might be to have this object also be final, and when a file
    // is moved, we actually create a new FileEpisode object for it, and replace the
    // old FileEpisode in the EpisodeDb and in the TableItem, and then drop the old
    // FileEpisode.  TODO?
    private File fileObj;

    // The state of this object, not the state of the actual TV episode.  This is
    // about how far we've processed the filename.
    private EpisodeStatus episodeStatus;
    private Show show;

    // We currently keep a link to the item in the view that represents this object.
    // It might be better to just have the view object subscribe to the episode, or
    // possibly to the map entry in the EpisodeDb, but this is simple for now.
    private TableItem viewItem = null;
    private EpisodeUIStatus uiStatus = EpisodeUIStatus.NEW;

    private Deque<EpisodeInformationListener> listeners = new ArrayDeque<>();

    // This class actually figures out the proposed new name for the file, so we need
    // a link to the user preferences to know how the user wants the file renamed.
    private UserPreferences userPrefs = UserPreferences.getInstance();

    public static String getExtension(File file) {
        String filename = file.getName();
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            return filename.substring(dot);
        }
        return "";
    }

    // Initially we create the FileEpisode with nothing more than the filename.
    // Other information will flow in.
    public FileEpisode(String filename) {
        fileObj = new File(filename);
        originalFilename = filename;
        filenameSuffix = getExtension(fileObj);
        episodeStatus = EpisodeStatus.UNPARSED;
    }

    public void listen(EpisodeInformationListener o) {
        listeners.push(o);
    }

    public String getEpisodeStatusString() {
        return episodeStatus.toString();
    }

    public boolean wasNotParsed() {
        return (episodeStatus == EpisodeStatus.BAD_PARSE);
    }

    public boolean wasParsed() {
        return (episodeStatus != EpisodeStatus.BAD_PARSE)
            && (episodeStatus != EpisodeStatus.UNPARSED);
    }

    public boolean isRenameInProgress() {
        return (episodeStatus == EpisodeStatus.MOVING);
    }

    public boolean isFailToParse() {
        return (episodeStatus == EpisodeStatus.BAD_PARSE);
    }

    public boolean isFailed() {
        return ((episodeStatus == EpisodeStatus.BAD_PARSE)
                || (episodeStatus == EpisodeStatus.UNFOUND)
                || (episodeStatus == EpisodeStatus.NO_LISTINGS)
                || (episodeStatus == EpisodeStatus.FAIL_TO_MOVE));
    }

    public boolean isInvestigating() {
        return (episodeStatus == EpisodeStatus.PARSED);
    }

    public boolean isNewlyAdded() {
        return (episodeStatus == EpisodeStatus.UNPARSED);
    }

    public boolean isDownloaded() {
        return (episodeStatus == EpisodeStatus.DOWNLOADED);
    }

    public boolean isReady() {
        return ((episodeStatus == EpisodeStatus.DOWNLOADED)
                || (episodeStatus == EpisodeStatus.RENAMED));
    }

    public boolean isShowReady() {
        // should include isReady()?
        return (episodeStatus == EpisodeStatus.GOT_SHOW);
    }

    public String getFilename() {
        return originalFilename;
    }

    public String getFilenameSuffix() {
        return filenameSuffix;
    }

    public String getFilenameShow() {
        return filenameShow;
    }

    public void setFilenameShow(String filenameShow) {
        this.filenameShow = filenameShow;
    }

    public void setRawShow(String filenameShow) {
        this.filenameShow
            = StringUtils.replacePunctuation(filenameShow).toLowerCase();
    }

    public int getFilenameSeason() {
        return filenameSeason;
    }

    public void setFilenameSeason(int filenameSeason) {
        this.filenameSeason = filenameSeason;
    }

    public void setFilenameSeason(String filenameSeasonString) {
        this.filenameSeason = Integer.parseInt(filenameSeasonString);
    }

    public int getFilenameEpisode() {
        return filenameEpisode;
    }

    public void setFilenameEpisode(int filenameEpisode) {
        this.filenameEpisode = filenameEpisode;
    }

    public void setFilenameEpisode(String filenameEpisodeString) {
        this.filenameEpisode = Integer.parseInt(filenameEpisodeString);
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

    public File getFile() {
        if (fileObj == null) {
            fileObj = new File(originalFilename);
        }
        return fileObj;
    }

    public void setFile(File fileObj) {
        this.fileObj = fileObj;
    }

    private void setStatus(EpisodeStatus newStatus) {
        episodeStatus = newStatus;
        for (EpisodeInformationListener l : listeners) {
            l.onEpisodeUpdate(this);
        }
    }

    public void setParsed() {
        setStatus(EpisodeStatus.PARSED);
    }

    public void setBadParse() {
        setStatus(EpisodeStatus.BAD_PARSE);
    }

    public void setDownloaded() {
        setStatus(EpisodeStatus.DOWNLOADED);
    }

    public void setMoving() {
        setStatus(EpisodeStatus.MOVING);
    }

    public void setRenamed() {
        setStatus(EpisodeStatus.RENAMED);
    }

    public void setFailToMove() {
        setStatus(EpisodeStatus.FAIL_TO_MOVE);
    }

    public void setDoesNotExist() {
        setStatus(EpisodeStatus.FAIL_TO_MOVE);
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
        if ((show == null) || (show instanceof FailedShow)) {
            setStatus(EpisodeStatus.UNFOUND);
        } else {
            setStatus(EpisodeStatus.GOT_SHOW);
        }
    }

    @Override
    public void downloadListingsComplete(Show show) {
        setStatus(EpisodeStatus.DOWNLOADED);
    }

    @Override
    public void downloadListingsFailed(Show show) {
        logger.warning("failed to download listings for " + show);
        setStatus(EpisodeStatus.NO_LISTINGS);
    }

    private void findEpisodes(final Show show) {
        ListingsLookup.getListings(show, this);
    }

    @Override
    public void downloadComplete(Show show) {
        setShow(show);
        findEpisodes(show);
    }

    @Override
    public void downloadFailed(Show show) {
        setShow(show);
    }

    // It would be nice to call this on our own.  But if we do, we have
    // a potential race condition where the UI gets an initial version of
    // the episode, then the episode gets its lookup information, and *then*
    // the UI registers as a listener.

    // One approach would be that registering as a listener automatically gets
    // you an immediate publish, but most of the time, that would be
    // unnecessary.  A more efficient approach would be to automatically call
    // lookupShow after the listener is added, knowing that, in reality, each
    // episode will have exactly one listener.  But that's a little too magical.

    // I prefer the pedantic approach of having the UI explicitly tell us when
    // it's all set, and we should go look up the data.

    // There's another approach that would do an immediate publish if and only
    // if the data is different from what the listener already knows about, but
    // that will have to wait.
    public void lookupShow() {
        if (episodeStatus == EpisodeStatus.UNPARSED) {
            TVRenamer.parseFilename(this);
        }
        if (filenameShow == null) {
            logger.info("cannot lookup show; did not extract a show name: "
                        + originalFilename);
        } else {
            ShowStore.mapStringToShow(filenameShow, this);
            logger.info("mapStringToShow returned for " + filenameShow);
        }
    }

    public TableItem getViewItem() {
        return viewItem;
    }

    public boolean setViewItem(TableItem newViewItem) {
        if (newViewItem == null) {
            uiStatus = EpisodeUIStatus.REMOVED;
        } else {
            if (viewItem != null) {
                logger.info("changing table item for episode! " + this);
            }
            Object data = newViewItem.getData();
            if ((data != null) && (data instanceof FileEpisode)) {
                if (this == (FileEpisode) data) {
                    // logger.info("setting back link from episode to item!!");
                    uiStatus = EpisodeUIStatus.ADDED;
                } else {
                    logger.info("setting table item which points to a different episode! " + this);
                    uiStatus = EpisodeUIStatus.ERROR;
                }
            } else {
                logger.info("setting table item which doesn't point back to me! " + this);
                uiStatus = EpisodeUIStatus.ERROR;
            }
        }
        viewItem = newViewItem;
        return (uiStatus != EpisodeUIStatus.ERROR);
    }

    private String seasonSubdir() {
        return userPrefs.getSeasonPrefix()
            + String.format((userPrefs.isSeasonPrefixLeadingZero() ? "%02d" : "%d"),
                            filenameSeason);
    }

    private String addDestinationDirectory(Show showObj, String basename) {
        String show = (showObj == null) ? filenameShow : showObj.getName();
        String dirname = StringUtils.sanitiseTitle(show);
        File destPath = new File(userPrefs.getDestinationDirectory(), dirname);

        // Defect #50: Only add the 'season #' folder if set, otherwise put files in showname root
        if (StringUtils.isNotBlank(userPrefs.getSeasonPrefix())) {
            destPath = new File(destPath, seasonSubdir());
        }
        File destFile = new File(destPath, basename);
        return destFile.getAbsolutePath();
    }

    private String transformedFilename(String officialShowName,
                                       String titleString,
                                       String seasonNumString,
                                       LocalDate airDate)
    {
        String nf = userPrefs.getRenameReplacementString();

        // Make whatever modifications are required
        nf = nf.replaceAll(SERIES_NAME, officialShowName);
        nf = nf.replaceAll(SEAS_NUMBER, seasonNumString);
        nf = nf.replaceAll(SNUM_LEADZR, new DecimalFormat("00").format(filenameSeason));
        nf = nf.replaceAll(EPISODE_NUM, new DecimalFormat("##0").format(filenameEpisode));
        nf = nf.replaceAll(ENUM_LEADZR, new DecimalFormat("#00").format(filenameEpisode));
        nf = nf.replaceAll(EPISD_TITLE, Matcher.quoteReplacement(titleString));
        nf = nf.replaceAll(EP_TIT_NOSP, Matcher.quoteReplacement(StringUtils.makeDotTitle(titleString)));
        nf = nf.replaceAll(ERESOLUTION, filenameResolution);

        // Date and times
        if (airDate == null) {
            nf = nf.replaceAll(DATEDAY_NUM, "");
            nf = nf.replaceAll(DATEDAY_NLZ, "");
            nf = nf.replaceAll(DATEMON_NUM, "");
            nf = nf.replaceAll(DATEMON_NLZ, "");
            nf = nf.replaceAll(DATE_YR_FUL, "");
            nf = nf.replaceAll(DATE_YR_MIN, "");
        } else {
            nf = replaceDate(nf, DATEDAY_NUM, airDate, "d");
            nf = replaceDate(nf, DATEDAY_NLZ, airDate, "dd");
            nf = replaceDate(nf, DATEMON_NUM, airDate, "M");
            nf = replaceDate(nf, DATEMON_NLZ, airDate, "MM");
            nf = replaceDate(nf, DATE_YR_FUL, airDate, "yyyy");
            nf = replaceDate(nf, DATE_YR_MIN, airDate, "yy");
        }
        // Note, this is an instance variable, not a local variable.
        fileBasename = StringUtils.sanitiseTitle(nf);

        nf = fileBasename + filenameSuffix;
        nf = StringUtils.sanitiseTitle(nf);

        return nf;
    }

    private String getOfficialShowName(Show show) {
        // Ensure that all special characters in the replacement are quoted
        String officialShowName = show.getName();
        officialShowName = Matcher.quoteReplacement(officialShowName);
        officialShowName = GlobalOverrides.getInstance().getShowName(officialShowName);

        return officialShowName;
    }

    private String getTitleString(Season season) {
        if (season != null) {
            try {
                return season.getTitle(filenameEpisode);
            } catch (EpisodeNotFoundException e) {
                logger.info("Episode not found for '" + this);
            }
        }
        return "";
    }

    private String getSeasonNumString(Season season) {
        if (season == null) {
            logger.log(Level.SEVERE, "Season #" + filenameSeason
                       + " not found for show '" + filenameShow + "'");
            return String.valueOf(filenameSeason);
        }
        return String.valueOf(season.getNumber());
    }

    private LocalDate getAirDate(Season season) {
        LocalDate airDate = null;
        if (season != null) {
            try {
                airDate = season.getAirDate(filenameEpisode);
            } catch (EpisodeNotFoundException ignored) {
            }
        }
        if (airDate == null) {
            logger.log(Level.WARNING, "Episode air date not found for '"
                       + this.toString() + "'");
        }
        return airDate;
    }

    public String getNewFilename() {
        if ((episodeStatus != EpisodeStatus.DOWNLOADED)
            && (episodeStatus != EpisodeStatus.RENAMED))
        {
            return null;
        }
        String newBasename = fileObj.getName();
        Show show = ShowStore.mapStringToShow(filenameShow);
        if (userPrefs.isRenameEnabled()) {
            Season season = show.getSeason(filenameSeason);
            newBasename = transformedFilename(getOfficialShowName(show),
                                              getTitleString(season),
                                              getSeasonNumString(season),
                                              getAirDate(season));
        }
        if (userPrefs.isMoveEnabled()) {
            return addDestinationDirectory(show, newBasename);
        } else {
            return newBasename;
        }
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

    @Override
    public String toString() {
        return "FileEpisode { show:"
                + filenameShow
                + ", season:"
                + filenameSeason
                + ", episode:"
                + filenameEpisode
                + ", file:"
                + fileObj.getName()
                + " }";
    }
}
