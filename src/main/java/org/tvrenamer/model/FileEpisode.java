// FileEpisode - represents a file on disk which is presumed to contain a single
//   episode of a TV series.
//
// This is a very mutable class.  It is initially created with just a filename,
// and then information comes streaming in.
//

package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.widgets.TableItem;
import org.tvrenamer.controller.EpisodeInformationListener;
import org.tvrenamer.controller.EpisodeListListener;
import org.tvrenamer.controller.FilenameParser;
import org.tvrenamer.controller.ListingsLookup;
import org.tvrenamer.controller.NameFormatter;
import org.tvrenamer.controller.SeriesLookup;
import org.tvrenamer.controller.SeriesLookupListener;
import org.tvrenamer.controller.util.FileUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileEpisode implements SeriesLookupListener, EpisodeListListener {

    private static Logger logger = Logger.getLogger(FileEpisode.class.getName());

    private enum ParseStatus {
        UNPARSED,
        PARSED,
        BAD_PARSE
    }

    private enum SeriesStatus {
        NOT_STARTED,
        QUERYING,
        GOT_SERIES,
        UNFOUND,
        NO_LISTINGS,
        PARSED_ALL
    }

    private enum FileStatus {
        NO_FILE,
        UNCHECKED,
        MOVING,
        RENAMED,
        FAIL_TO_MOVE
    }

    // This separate enum refers to whether or not the FileEpisode is showing in the UI.
    private enum EpisodeUIStatus {
        NEW,
        ADDED,
        REMOVED,
        ERROR
    }

    // Although we also store the path object that clearly gives us all the information
    // about the file's name, store this explicitly to know what we were given.  Note,
    // even after the file is moved, this value remains the same.  Note also this could
    // be a non-normalized, relative pathstring, for example.
    private final String originalFilepath;
    private String currentLocation;

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

    private String seriesName = null;
    private String fileBasename = null;

    private int seasonNum = 0;
    private int episodeNum = 0;

    // The "pathObj" is the java.nio.files.Path object representing the file that this
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
    private Path pathObj;
    private long fileSize;
    private static final long NO_FILE_SIZE = -1L;

    // The state of this object, not the state of the actual TV episode.  This is
    // about how far we've processed the filename.
    private ParseStatus parseStatus = ParseStatus.UNPARSED;
    private SeriesStatus seriesStatus = SeriesStatus.NOT_STARTED;
    private FileStatus fileStatus = FileStatus.UNCHECKED;

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

    public static String getExtension(Path path) {
        Path basename = path.getFileName();
        if (basename == null) {
            return EMPTY_STRING;
        }
        String filename = basename.toString();
        int dot = filename.lastIndexOf('.');
        // We assume that an "extension" only exists if there's something to extend --
        // i.e., a base name.  If the last dot is the first character
        if (dot > 0) {
            return filename.substring(dot);
        }
        return EMPTY_STRING;
    }

    // Initially we create the FileEpisode with nothing more than the file and
    // possibly a listener.  Other information will flow in.
    public FileEpisode(Path path,
                       TableItem item,
                       EpisodeInformationListener listener)
    {
        viewItem = item;
        originalFilepath = path.toString();
        currentLocation = originalFilepath;

        filenameSuffix = getExtension(path);

        if (item == null) {
            uiStatus = EpisodeUIStatus.NEW;
        } else {
            item.setData(this);
            uiStatus = EpisodeUIStatus.ADDED;
        }

        setPath(path);

        if (listener != null) {
            listeners.push(listener);
            listener.onEpisodeUpdate(this);
            lookupSeries();
        }
    }

    // Initially we create the FileEpisode with nothing more than the filename.
    // Other information will flow in.
    public FileEpisode(Path path) {
        this(path, null, null);
    }

    public FileEpisode(String filename) {
        this(Paths.get(filename));
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
        if (parseStatus == ParseStatus.PARSED) {
            if (seriesStatus == SeriesStatus.NOT_STARTED) {
                if (seriesName != null) {
                    // logger.fine("looking up based on match " + seriesName);
                    seriesStatus = SeriesStatus.QUERYING;
                    SeriesLookup.mapStringToShow(seriesName, this);
                } else if (filenameSeries != null) {
                    // logger.warning("looking up based on query string " + filenameSeries);
                    seriesStatus = SeriesStatus.QUERYING;
                    SeriesLookup.mapStringToShow(filenameSeries, this);
                } else {
                    seriesStatus = SeriesStatus.NOT_STARTED;
                    logger.info("cannot lookup series; did not extract a series name: "
                                + originalFilepath);
                }
            }
        }
    }

    public void listen(EpisodeInformationListener o) {
        listeners.push(o);
    }

    public boolean wasNotParsed() {
        return (parseStatus == ParseStatus.BAD_PARSE);
    }

    public boolean wasParsed() {
        return (parseStatus == ParseStatus.PARSED);
    }

    public boolean isRenameInProgress() {
        return (fileStatus == FileStatus.MOVING);
    }

    public boolean isFailToParse() {
        return (parseStatus == ParseStatus.BAD_PARSE);
    }

    public boolean isFailToMove() {
        return (fileStatus == FileStatus.FAIL_TO_MOVE);
    }

    public boolean isFailed() {
        return ((parseStatus == ParseStatus.BAD_PARSE)
                || (seriesStatus == SeriesStatus.UNFOUND)
                || (seriesStatus == SeriesStatus.NO_LISTINGS)
                || (fileStatus == FileStatus.FAIL_TO_MOVE));
    }

    public boolean isInvestigating() {
        return ((seriesStatus == SeriesStatus.QUERYING)
                || (seriesStatus == SeriesStatus.GOT_SERIES));
    }

    public boolean isNewlyAdded() {
        return (parseStatus == ParseStatus.UNPARSED);
    }

    public boolean isDownloaded() {
        return (seriesStatus == SeriesStatus.PARSED_ALL);
    }

    public boolean isRenamed() {
        return (fileStatus == FileStatus.RENAMED);
    }

    public boolean isReady() {
        if ((seasonNum == 0) || (episodeNum == 0)) {
            return false;
        }
        return ((parseStatus == ParseStatus.PARSED)
                && (seriesStatus == SeriesStatus.PARSED_ALL)
                && ((fileStatus == FileStatus.UNCHECKED)
                    || (fileStatus == FileStatus.RENAMED)));
    }

    public boolean isSeriesReady() {
        // TODO: should include isReady()?
        return (seriesStatus == SeriesStatus.GOT_SERIES);
    }

    public String getFilepath() {
        return currentLocation;
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

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
        seriesStatus = SeriesStatus.NOT_STARTED;
        lookupSeries();
    }

    public String getSeriesName() {
        return seriesName;
    }

    public String getSeriesInformationString() {
        String display = "";
        if (series == null) {
            display += "(not found)";
        } else {
            display += series.getName() + " [" + series.getIdString() + "]; ";
        }
        if (seriesName == null) {
            display += "parse: " + filenameSeries;
        } else {
            display += "show: " + seriesName;
        }

        return display;
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

    public String makeEpisodeId() {
        // TODO: use getRenameReplacementString() from Renamer

        return "S" + NameFormatter.TWO_DIGITS.get().format(seasonNum)
            + "E" + NameFormatter.TWO_DIGITS.get().format(episodeNum);
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

    public Path getPath() {
        return pathObj;
    }

    public void setPath(Path pathObj) {
        Path oldPath = this.pathObj;
        this.pathObj = pathObj;
        try {
            fileSize = Files.size(pathObj);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "couldn't get size of " + pathObj);
            fileSize = NO_FILE_SIZE;
        }
        currentLocation = pathObj.toAbsolutePath().toString();
        boolean isParsed = FilenameParser.parseFilename(this);
        if (isParsed) {
            parseStatus = ParseStatus.PARSED;
        } else {
            parseStatus = ParseStatus.BAD_PARSE;
        }
        if ((oldPath != null) && FileUtilities.differentFiles(oldPath, pathObj)) {
            fileStatus = FileStatus.RENAMED;
        }
        update();
    }

    public long getFileSize() {
        return fileSize;
    }

    private void update() {
        for (EpisodeInformationListener l : listeners) {
            l.onEpisodeUpdate(this);
        }
    }

    public void setMoving() {
        if (fileStatus != FileStatus.MOVING) {
            fileStatus = FileStatus.MOVING;
            update();
        }
    }

    public void setRenamed() {
        if (fileStatus != FileStatus.RENAMED) {
            fileStatus = FileStatus.RENAMED;
            update();
        }
    }

    public void setFailToMove() {
        if (fileStatus != FileStatus.FAIL_TO_MOVE) {
            fileStatus = FileStatus.FAIL_TO_MOVE;
            update();
        }
    }

    public void setDoesNotExist() {
        if (fileStatus != FileStatus.NO_FILE) {
            fileStatus = FileStatus.NO_FILE;
            update();
        }
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
        if ((series == null) || (series instanceof UnresolvedShow)) {
            if (seriesStatus != SeriesStatus.UNFOUND) {
                seriesStatus = SeriesStatus.UNFOUND;
                update();
            }
        } else {
            seriesName = series.getName();
            if (seriesStatus != SeriesStatus.GOT_SERIES) {
                seriesStatus = SeriesStatus.GOT_SERIES;
                update();
            }
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
        if (season == null) {
            seriesStatus = SeriesStatus.NO_LISTINGS;
        } else {
            seriesStatus = SeriesStatus.PARSED_ALL;
        }
    }

    @Override
    public void downloadListingsComplete(Series series) {
        // TODO: we already have the series.  We don't need to return it.
        setSeason();
        // Only thing we could do would be to verify it.
        update();
    }

    @Override
    public void downloadListingsFailed(Series series) {
        logger.warning("failed to download listings for " + series);
        if (seriesStatus != SeriesStatus.NO_LISTINGS) {
            seriesStatus = SeriesStatus.NO_LISTINGS;
            update();
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
                logger.finer("changing table item for episode! " + this);
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

    public long getAirDate() {
        if (series == null) {
            return 0L;
        }
        if (season == null) {
            return 0L;
        }

        Episode episode = season.getEpisode(episodeNum);
        if (episode == null) {
            return 0L;
        }

        long epochDay = 1 + episode.getAirDate().toEpochDay();
        return epochDay * 24L * 60L * 60L * 1000L;
    }

    public Path getDestinationDirectory() {
        return formatter.getDestinationDirectory();
    }

    public String getFileBasename() {
        // Note, this is an instance variable, not a local variable.
        fileBasename = formatter.getNewBasename();
        return fileBasename;
    }

    public String getProposedFilename() {
        if (series == null) {
            return ADDED_PLACEHOLDER_FILENAME;
        }
        if (season == null) {
            return PROCESSING_EPISODES;
        }
        if (0L == getAirDate()) {
            seriesStatus = SeriesStatus.UNFOUND;
            update();
            return NO_SUCH_SHOW_MESSAGE;
        }
        if (formatter == null) {
            formatter = new NameFormatter(this);
        }
        if (userPrefs.isRenameEnabled()) {
            // Note, this is an instance variable, not a local variable.
            fileBasename = formatter.getNewBasename();
            return formatter.getProposedFilename(fileBasename + filenameSuffix);
        } else {
            return formatter.getProposedFilename(pathObj.getFileName().toString());
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
                + pathObj.getFileName()
                + " }";
    }
}
