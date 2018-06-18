package org.tvrenamer.model.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class Constants {

    public static final Locale THIS_LOCALE = Locale.getDefault();

    public static final String APPLICATION_NAME = "TVRenamer";

    public static final String VERSION_NUMBER = Environment.readVersionNumber();

    public static final String TVRENAMER_PROJECT_URL = "http://tvrenamer.org";
    public static final String TVRENAMER_DOWNLOAD_URL = TVRENAMER_PROJECT_URL + "/downloads";

    public static final String NO_SUCH_SHOW_MESSAGE = "No such show found: ";
    public static final String DOWNLOADING_FAILED_MESSAGE = "Downloading show listings failed.  "
        + "Check internet connection";
    public static final String FAIL_MSG_FOR_NONFAIL = "Internal error: fail message for non-failure";
    public static final String ADDED_PLACEHOLDER_FILENAME = "Downloading ...";
    public static final String CANT_PARSE_FILENAME = "filename not parsed";
    public static final String BROKEN_PLACEHOLDER_FILENAME = "Unable to download show information";

    public static final String PROCESSING_EPISODES = "Processing episodes...";
    public static final String CHECKBOX_LABEL = "Selected";
    public static final String FILENAME_LABEL = "Current File";
    public static final String STATUS_LABEL = "Status";
    public static final String EXIT_LABEL = "Exit";
    public static final String PREFS_LABEL = "Preferences";
    public static final String FILE_MENU_LABEL = "File";
    public static final String ABOUT_MENU_LABEL = "About";
    public static final String QUIT_LABEL = "Quit";
    public static final String HELP_LABEL = "Help";
    public static final String ERROR_LABEL = "Error";
    public static final String ADD_FILES_LABEL = "Add files";
    public static final String ADD_FOLDER_LABEL = "Add Folder";
    public static final String CLEAR_LIST_LABEL = "Clear List";
    public static final String SELECT_ALL_LABEL = "Select All";
    public static final String DESELECT_ALL_LABEL = "Deselect All";
    public static final String PROGRESS_THREAD_LABEL = "ProgressBarThread";
    public static final String RENAME_FAILED_LABEL = "Rename Failed";
    public static final String RENAME_LABEL = "Rename Selected";
    public static final String RENAME_AND_MOVE = "Rename && Move Selected";
    public static final String MOVE_HEADER = "Proposed File Path";
    public static final String RENAME_HEADER = "Proposed File Name";
    public static final String RENAME_TOOLTIP = "Clicking this button will rename the selected "
        + "files but leave them where they are.";
    public static final String NO_DND = "Drag and Drop is not currently supported "
        + "on your operating system, please use the 'Browse Files' option above";

    public static final String MOVE_TOOLTIP_1 = "Clicking this button will rename and move "
        + "the selected files to the directory set in preferences (currently ";
    public static final String MOVE_TOOLTIP_2 = ").";
    public static final String UPDATE_IS_AVAILABLE_1 = "There is an update available. <a href=\"";
    public static final String UPDATE_IS_AVAILABLE_2 = "\">Click here to download</a>";

    public static final String UNKNOWN_EXCEPTION = "An error occurred, please check "
        + "your internet connection, java version or run from the command line to show errors";
    public static final String ERROR_PARSING_XML = "Error parsing XML";
    public static final String VISIT_WEBPAGE = "Visit Webpage";
    public static final String ERROR_DOWNLOADING_SHOW_INFORMATION = "Error downloading "
        + "show information. Check internet or proxy settings";


    public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%0e] %t";
    public static final String CANT_PARSE_EPISODE_TEXT = "n/a";
    public static final String HAVENT_PARSED_EPISODE_TEXT = "---";
    public static final String DEFAULT_SEASON_PREFIX = "Season ";

    public static final String ICON_PARENT_DIRECTORY = "res";
    public static final String TVRENAMER_ICON_PATH = "/icons/tvrenamer.png";

    private static final String CONFIGURATION_DIRECTORY_NAME = ".tvrenamer";
    private static final String PREFERENCES_FILENAME = "prefs.xml";
    private static final String OVERRIDES_FILENAME = "overrides.xml";
    private static final String KNOWN_SHOWS_FILENAME = "shows.xml";
    private static final String TVDB_CACHE_DIRNAME = "thetvdb";

    public static final String DEVELOPER_DEFAULT_OVERRIDES_FILENAME = "etc/default-overrides.xml";

    public static final String IMDB_BASE_URL = "http://www.imdb.com/title/";

    public static final Path USER_HOME_DIR = Paths.get(Environment.USER_HOME);

    public static final Path DEFAULT_DESTINATION_DIRECTORY = USER_HOME_DIR.resolve("TV");
    public static final Path CONFIGURATION_DIRECTORY
        = USER_HOME_DIR.resolve(CONFIGURATION_DIRECTORY_NAME);
    public static final Path PREFERENCES_FILE
        = CONFIGURATION_DIRECTORY.resolve(PREFERENCES_FILENAME);
    public static final Path OVERRIDES_FILE
        = CONFIGURATION_DIRECTORY.resolve(OVERRIDES_FILENAME);
    public static final Path THETVDB_CACHE
        = CONFIGURATION_DIRECTORY.resolve(TVDB_CACHE_DIRNAME);
    public static final Path KNOWN_SHOWS_FILE
        = CONFIGURATION_DIRECTORY.resolve(KNOWN_SHOWS_FILENAME);

    public static final Path PREFERENCES_FILE_LEGACY
        = USER_HOME_DIR.resolve("tvrenamer.preferences");
    public static final Path OVERRIDES_FILE_LEGACY
        = USER_HOME_DIR.resolve(".tvrenameroverrides");

    public static final String EMPTY_STRING = "";
}
