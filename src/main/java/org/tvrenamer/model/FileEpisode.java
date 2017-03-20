// FileEpisode - represents a file on disk which is presumed to contain a single
//   episode of a TV series.
//
// This is a very mutable class.  It is initially created with just a filename,
// and then information comes streaming in.
//

package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.ADDED_PLACEHOLDER_FILENAME;

import org.eclipse.swt.widgets.TableItem;
import org.tvrenamer.controller.EpisodeInformationListener;
import org.tvrenamer.controller.FilenameParser;
import org.tvrenamer.controller.ListingsLookup;
import org.tvrenamer.controller.NameFormatter;
import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.ShowListingsListener;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

public class FileEpisode implements ShowInformationListener, ShowListingsListener {

    private static Logger logger = Logger.getLogger(FileEpisode.class.getName());

    private enum EpisodeStatus {
        UNPARSED,
        PARSED,
        BAD_PARSE,

        GOT_SERIES,
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

    // Although we also store the file object that clearly gives us all the information
    // about the file's name, store this explicitly to know what we were given.  Note,
    // even after the file is moved, this value remains the same.  Note also this could
    // be a non-normalized, relative pathstring, for example.
    private final String originalFilename;

    // "filename" instance vars -- these are the results of parsing the filename.
    // The "filenameSeries" is the precise string from the filename, that we think
    // corresponds to the name of the TV series.  We use these data to look up
    // information about the series, and the information we find may differ from
    // what the filename actually represents.
    private final String filenameSuffix;
    private String filenameSeries;
    private String filenameSeason;
    private String filenameEpisode;
    private String filenameResolution = "";

    private String fileBasename = null;

    private int seasonNum = 0;
    private int episodeNum = 0;

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
    private Series series;
    private Season season;

    // We currently keep a link to the item in the view that represents this object.
    // It might be better to just have the view object subscribe to the episode, or
    // possibly to the map entry in the EpisodeDb, but this is simple for now.
    private TableItem viewItem = null;
    private EpisodeUIStatus uiStatus = EpisodeUIStatus.NEW;

    private Deque<EpisodeInformationListener> listeners = new ArrayDeque<>();

    // This class actually figures out the proposed new name for the file, so we need
    // a link to the user preferences to know how the user wants the file renamed.
    private UserPreferences userPrefs = UserPreferences.getInstance();

    private NameFormatter formatter = null;

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
        episodeStatus = EpisodeStatus.UNPARSED;
        originalFilename = filename;
        fileObj = new File(filename);
        filenameSuffix = getExtension(fileObj);
        FilenameParser.parseFilename(this);
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
        if ((seasonNum == 0) || (episodeNum == 0)) {
            return false;
        }
        return ((episodeStatus == EpisodeStatus.DOWNLOADED)
                || (episodeStatus == EpisodeStatus.RENAMED));
    }

    public boolean isSeriesReady() {
        // should include isReady()?
        return (episodeStatus == EpisodeStatus.GOT_SERIES);
    }

    public String getFilename() {
        return originalFilename;
    }

    public String getFilenameSuffix() {
        return filenameSuffix;
    }

    public String getFilenameSeries() {
        return filenameSeries;
    }

    public void setRawSeries(String filenameSeries) {
        this.filenameSeries = filenameSeries;
    }

    public String getFilenameSeason() {
        return filenameSeason;
    }

    public int getSeasonNum() {
        return seasonNum;
    }

    public void setFilenameSeason(String filenameSeason) {
        this.filenameSeason = filenameSeason;
        try {
            seasonNum = Integer.parseInt(filenameSeason);
        } catch (Exception e) {
            seasonNum = 0;
        }
    }

    public String getFilenameEpisode() {
        return filenameEpisode;
    }

    public int getEpisodeNum() {
        return episodeNum;
    }

    public void setFilenameEpisode(String filenameEpisode) {
        this.filenameEpisode = filenameEpisode;
        try {
            episodeNum = Integer.parseInt(filenameEpisode);
        } catch (Exception e) {
            episodeNum = 0;
        }
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

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
        if ((series == null) || (series instanceof UnresolvedShow)) {
            setStatus(EpisodeStatus.UNFOUND);
        } else {
            setStatus(EpisodeStatus.GOT_SERIES);
        }
    }

    @Override
    public void downloadComplete(Series series) {
        setSeries(series);
        ListingsLookup.getListings(series, this);
    }

    @Override
    public void downloadFailed(Series series) {
        setSeries(series);
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason() {
        if (series != null) {
            season = series.getSeason(filenameSeason);
        }
    }

    @Override
    public void downloadListingsComplete(Series series) {
        // TODO: we already have the series.  We don't need to return it.
        setSeason();
        // Only thing we could do would be to verify it.
        setStatus(EpisodeStatus.DOWNLOADED);
    }

    @Override
    public void downloadListingsFailed(Series series) {
        logger.warning("failed to download listings for " + series);
        setStatus(EpisodeStatus.NO_LISTINGS);
    }

    // It would be nice to call this on our own.  But if we do, we have
    // a potential race condition where the UI gets an initial version of
    // the episode, then the episode gets its lookup information, and *then*
    // the UI registers as a listener.

    // One approach would be that registering as a listener automatically gets
    // you an immediate publish, but most of the time, that would be
    // unnecessary.  A more efficient approach would be to automatically call
    // lookupSeries after the listener is added, knowing that, in reality, each
    // episode will have exactly one listener.  But that's a little too magical.

    // I prefer the pedantic approach of having the UI explicitly tell us when
    // it's all set, and we should go look up the data.

    // There's another approach that would do an immediate publish if and only
    // if the data is different from what the listener already knows about, but
    // that will have to wait.
    public void lookupSeries() {
        if (filenameSeries == null) {
            logger.info("cannot lookup series; did not extract a series name: "
                        + originalFilename);
        } else {
            ShowStore.mapStringToShow(filenameSeries, this);
            logger.fine("mapStringToShow returned for " + filenameSeries);
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

    public String getProposedFilename() {
        if ((series == null) || (season == null)) {
            return ADDED_PLACEHOLDER_FILENAME;
        }
        if (formatter == null) {
            formatter = new NameFormatter(this);
        }
        if (userPrefs.isRenameEnabled()) {
            // Note, this is an instance variable, not a local variable.
            fileBasename = formatter.getNewBasename();
            return formatter.getProposedFilename(fileBasename + filenameSuffix);
        } else {
            return formatter.getProposedFilename(fileObj.getName());
        }
    }

    @Override
    public String toString() {
        return "FileEpisode { series:"
                + filenameSeries
                + ", season:"
                + filenameSeason
                + ", episode:"
                + filenameEpisode
                + ", file:"
                + fileObj.getName()
                + " }";
    }
}
