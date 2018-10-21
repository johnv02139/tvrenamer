package org.tvrenamer.model;

import org.eclipse.swt.widgets.TableItem;
import org.tvrenamer.controller.EpisodeInformationListener;
import org.tvrenamer.controller.ShowInformationListener;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class FileEpisode implements ShowInformationListener {
    private static Logger logger = Logger.getLogger(FileEpisode.class.getName());

    public static final int NO_SEASON = -1;
    public static final int NO_EPISODE = -1;

    private static final String DATEDAY_NUM = ReplacementToken.DATE_DAY_NUM.getToken();
    private static final String DATEDAY_NLZ = ReplacementToken.DATE_DAY_NUMLZ.getToken();
    private static final String DATEMON_NUM = ReplacementToken.DATE_MONTH_NUM.getToken();
    private static final String DATEMON_NLZ = ReplacementToken.DATE_MONTH_NUMLZ.getToken();
    private static final String DATE_YR_FUL = ReplacementToken.DATE_YEAR_FULL.getToken();
    private static final String DATE_YR_MIN = ReplacementToken.DATE_YEAR_MIN.getToken();
    private static final String EPISODE_NUM = ReplacementToken.EPISODE_NUM.getToken();
    private static final String ENUM_LEADZR = ReplacementToken.EPISODE_NUM_LEADING_ZERO.getToken();
    private static final String EPISD_TITLE = ReplacementToken.EPISODE_TITLE.getToken();
    private static final String EP_TIT_NOSP = ReplacementToken.EPISODE_TITLE_NO_SPACES.getToken();
    private static final String SEAS_NUMBER = ReplacementToken.SEASON_NUM.getToken();
    private static final String SNUM_LEADZR = ReplacementToken.SEASON_NUM_LEADING_ZERO.getToken();
    private static final String SERIES_NAME = ReplacementToken.SHOW_NAME.getToken();

    // "filename" instance vars -- these are the results of parsing the filename.
    // The "filenameShow" is the precise string from the filename, that we think
    // corresponds to the name of the TV series.  We use these data to look up
    // information about the show, and the information we find may differ from
    // what the filename actually represents.
    private String filenameShow;
    private final int filenameSeason;
    private final int filenameEpisode;
    private final String filenameSuffix;

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
    private EpisodeInfo episodeStatus;
    private Deque<Show> shows;

    // We currently keep a link to the item in the view that represents this object.
    // It might be better to just have the view object subscribe to the episode, or
    // possibly to the map entry in the EpisodeDb, but this is simple for now.
    private TableItem viewItem = null;

    private Deque<EpisodeInformationListener> listeners = new ArrayDeque<>();

    // This class actually figures out the proposed new name for the file, so we need
    // a link to the user preferences to know how the user wants the file renamed.
    private UserPreferences userPrefs = UserPreferences.getInstance();

    public FileEpisode(String filenameShow,
                       int filenameSeason,
                       int filenameEpisode,
                       String filenameSuffix,
                       File fileObj)
    {
        this.filenameShow = filenameShow;
        this.filenameSeason = filenameSeason;
        this.filenameEpisode = filenameEpisode;
        this.filenameSuffix = filenameSuffix;
        this.fileObj = fileObj;
        episodeStatus = EpisodeInfo.ADDED;
    }

    // Obviously any code could call this constructor, but by convention we assume
    // that if this version is called, it means we tried to parse the filename and
    // failed to do so.  That's why we set the status to NOPARSE.
    public FileEpisode(String filenameSuffix, File fileObj) {
        this.filenameShow = "";
        this.filenameSeason = NO_SEASON;
        this.filenameEpisode = NO_EPISODE;
        this.filenameSuffix = filenameSuffix;
        this.fileObj = fileObj;
        episodeStatus = EpisodeInfo.NOPARSE;
    }

    public void listen(EpisodeInformationListener o) {
        listeners.push(o);
    }

    public boolean wasNotParsed() {
        return (episodeStatus == EpisodeInfo.NOPARSE);
    }

    public boolean wasParsed() {
        return (episodeStatus != EpisodeInfo.NOPARSE);
    }

    public String getFilenameShow() {
        return filenameShow;
    }

    public void setFilenameShow(String filenameShow) {
        this.filenameShow = filenameShow;
    }

    public int getFilenameSeason() {
        return filenameSeason;
    }

    public int getFilenameEpisode() {
        return filenameEpisode;
    }

    public String getFilenameSuffix() {
        return filenameSuffix;
    }

    public File getFile() {
        return fileObj;
    }

    public String getFilename() {
        return fileObj.getPath();
    }

    public void setFile(File fileObj) {
        this.fileObj = fileObj;
    }

    public EpisodeInfo getStatus() {
        return episodeStatus;
    }

    public void setStatus(EpisodeInfo newStatus) {
        logger.fine("setting status");
        episodeStatus = newStatus;
        for (EpisodeInformationListener l : listeners) {
            logger.fine("updating listener " + l);
            l.onEpisodeUpdate(this);
        }
    }

    public Deque<Show> getShows() {
        return shows;
    }

    public void setShows(List<Show> newShows) {
        logger.fine("setting shows");
        boolean fails = false;
        this.shows = new ArrayDeque<Show>();
        if ((newShows == null) || (newShows.size() == 0)) {
            fails = true;
        } else {
            for (Show s : newShows) {
                this.shows.addLast(s);
                if (s instanceof FailedShow) {
                    fails = true;
                }
            }
        }

        logger.fine("going to set status");
        if (fails) {
            setStatus(EpisodeInfo.BROKEN);
        } else {
            setStatus(EpisodeInfo.DOWNLOADED);
        }
    }

    @Override
    public void downloadComplete(List<Show> shows) {
        setShows(shows);
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
        ShowStore.mapStringToShows(filenameShow, this);
    }

    public TableItem getTableItem() {
        return viewItem;
    }

    public void setTableItem(TableItem newViewItem) {
        if (newViewItem != null) {
            if (viewItem != null) {
                logger.fine("changing table item for episode! " + this);
            }
            Object data = newViewItem.getData();
            if ((data != null) && (data instanceof FileEpisode)) {
                if (this == (FileEpisode) data) {
                    // logger.fine("setting back link from episode to item!!");
                } else {
                    logger.fine("setting table item which points to a different episode! " + this);
                }
            } else {
                logger.fine("setting table item which doesn't point back to me! " + this);
            }
        }
        viewItem = newViewItem;
    }

    public String getOfficialShowName(Show show) {
        // Ensure that all special characters in the replacement are quoted
        String officialShowName = show.getName();
        officialShowName = Matcher.quoteReplacement(officialShowName);
        officialShowName = GlobalOverrides.getInstance().getShowName(officialShowName);

        return officialShowName;
    }

    public String getShowId(String officialShowName) {
        if ((shows == null) || (officialShowName == null)) {
            logger.fine("null info for getShowId");
            return "";
        }
        for (Show s : shows) {
            if (officialShowName.equals(s.getName())) {
                logger.fine("match for getShowId! " + s);
                return s.getId();
            }
        }
        logger.fine("no match for getShowId");
        return "";
    }

    @Override
    public String toString() {
        return "FileEpisode { title:"
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
