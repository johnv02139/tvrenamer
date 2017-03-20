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
    public static final String TVRENAMER_ISSUES_URL = TVRENAMER_PROJECT_URL + "/issues";
    public static final String TVRENAMER_VERSION_URL = TVRENAMER_PROJECT_URL + "/version";

    public static final String NO_SUCH_SHOW_MESSAGE = "No such show found: ";
    public static final String DOWNLOADING_FAILED_MESSAGE = "Downloading show listings failed.  "
        + "Check internet connection";
    public static final String FAIL_TO_MOVE_MESSAGE = "Unable to move file to proposed destination";
    public static final String FAIL_MSG_FOR_NONFAIL = "Internal error: fail message for non-failure";
    public static final String ADDED_PLACEHOLDER_FILENAME = "Downloading ...";
    public static final String CANT_PARSE_FILENAME = "filename not parsed";
    public static final String BROKEN_PLACEHOLDER_FILENAME = "Unable to download show information";

    public static final String PROCESSING_EPISODES = "Processing episodes...";
    public static final String CHECKBOX_LABEL = "Selected";
    public static final String FILENAME_LABEL = "Current File";
    public static final String STATUS_LABEL = "Status";
    public static final String EXIT_LABEL = "Exit";
    public static final String PREFERENCES_LABEL = "Preferences";
    public static final String FILE_MENU_LABEL = "File";
    public static final String ABOUT_MENU_LABEL = "About";
    public static final String QUIT_LABEL = "Quit";
    public static final String HELP_LABEL = "Help";
    public static final String OK_LABEL = "OK";
    public static final String CLOSE_LABEL = "Close";
    public static final String CANCEL_LABEL = "Cancel";
    public static final String SAVE_LABEL = "Save";
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

    public static final String VERSION_LABEL = "Version: " + VERSION_NUMBER;
    public static final String ABOUT_TEXT = "About TVRenamer";
    public static final String TVRENAMER_DESCRIPTION = "TVRenamer is a Java GUI utility to rename "
        + "TV episodes from TV listings";
    public static final String PROJECT_TEXT = "<a href=\"" + TVRENAMER_PROJECT_URL
        + "\">Project Page</a>";
    public static final String TVRENAMER_SUPPORT_EMAIL = "support@tvrenamer.org";
    public static final String EMAIL_TEXT = "<a href=\"mailto:"
        + TVRENAMER_SUPPORT_EMAIL + "\">Send support email</a>";
    public static final String EMAIL_LINK = "mailto:" + TVRENAMER_SUPPORT_EMAIL;
    public static final String UPDATE_TEXT = "Check for Updates...";
    public static final String NEW_VERSION_TITLE = "New Version Available!";
    public static final String NEW_VERSION_AVAILABLE = "There is a new version available!\n\n"
        + "You are currently running " + VERSION_NUMBER + ", but there is an update available\n\n"
        + "Please visit " + TVRENAMER_PROJECT_URL + " to download the new version.";
    public static final String NO_NEW_VERSION_TITLE = "No New Version Available";
    public static final String NO_NEW_VERSION_AVAILABLE = "There is a no new version available\n\n"
        + "Please check the website (" + TVRENAMER_PROJECT_URL + ") for any news or check back later.";
    public static final String TVRENAMER_LICENSE_URL = "http://www.gnu.org/licenses/gpl-2.0.html";
    public static final String LICENSE_TEXT = "Licensed under the <a href=\""
        + TVRENAMER_LICENSE_URL + "\">GNU General Public License v2</a>";
    public static final String ISSUE_TRACKER = "<a href=\"" + TVRENAMER_ISSUES_URL
        + "\">Issue Tracker</a>";
    public static final String TVRENAMER_REPOSITORY_URL = "http://tvrenamer.org/source";
    public static final String REPOSITORY_TEXT = "<a href=\"" + TVRENAMER_REPOSITORY_URL
        + "\">Source Code</a>";

    public static final String REPLACEMENT_OPTIONS_LIST_ENTRY_REGEX = "(.*) :.*";
    public static final String IGNORE_WORDS_SPLIT_REGEX = "\\s*,\\s*";
    public static final String GENERAL_LABEL = "General";
    public static final String RENAMING_LABEL = "Renaming";
    public static final String MOVE_ENABLED_TEXT = "Move Enabled [?]";
    public static final String DEST_DIR_TEXT = "TV Directory [?]";
    public static final String DEST_DIR_BUTTON_TEXT = "Select directory";
    public static final String DIR_DIALOG_TEXT = "Please select a directory and click OK";
    public static final String SEASON_PREFIX_TEXT = "Season Prefix [?]";
    public static final String SEASON_PREFIX_ZERO_TEXT = "Season Prefix Leading Zero [?]";
    public static final String IGNORE_LABEL_TEXT = "Ignore files containing [?]";
    public static final String RECURSE_FOLDERS_TEXT = "Recursively add shows in subdirectories";
    public static final String CHECK_UPDATES_TEXT = "Check for Updates at startup";
    public static final String RENAME_TOKEN_TEXT = "Rename Tokens [?]";
    public static final String RENAME_FORMAT_TEXT = "Rename Format [?]";
    public static final String HELP_TOOLTIP = "Hover mouse over [?] to get help";
    public static final String GENERAL_TOOLTIP = " - TVRenamer will automatically move the files "
        + "to your 'TV' folder if you want it to.  \n"
        + " - It will move the file to <tv directory>/<show name>/<season prefix> #/ \n"
        + " - Once enabled, set the location below.";
    public static final String MOVE_ENABLED_TOOLTIP = "Whether the "
        + "'move to TV location' functionality is enabled";
    public static final String DEST_DIR_TOOLTIP = "The location of your 'TV' folder";
    public static final String PREFIX_TOOLTIP = " - The prefix of the season when renaming and "
        + "moving the file.  It is usually \"Season \" or \"s'\".\n - If no value is entered "
        + "(or \"\"), the season folder will not be created, putting all files in the show name "
        + "folder\n - The \" will not be included, just displayed here to show whitespace";
    public static final String SEASON_PREFIX_ZERO_TOOLTIP = "Whether to have a leading zero "
        + "in the season prefix";
    public static final String IGNORE_LABEL_TOOLTIP = "Provide comma separated list of words "
        + "that will cause a file to be ignored if they appear in the file's path or name.";
    public static final String RENAME_TOKEN_TOOLTIP = " - These are the possible tokens to "
        + " make up the 'Rename Format' below.\n"
        + " - You can drag and drop tokens to the 'Rename Format' text box below";
    public static final String RENAME_FORMAT_TOOLTIP = "The result of the rename, with the "
        + "tokens being replaced by the meaning above";
    public static final String CANT_CREATE_DEST = "Unable to create the destination directory";

    public static final String UNKNOWN_EXCEPTION = "An error occurred, please check "
        + "your internet connection, java version or run from the command line to show errors";
    public static final String ERROR_PARSING_XML = "Error parsing XML";
    public static final String VISIT_WEBPAGE = "Visit Webpage";
    public static final String ERROR_DOWNLOADING_SHOW_INFORMATION = "Error downloading "
        + "show information. Check internet or proxy settings";
    public static final String UNABLE_TO_CONNECT = "Unable connect to the TV listing website, "
        + "please check your internet connection.\nNote that proxies are not currently supported.";

    public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%0e] %t";
    public static final String CANT_PARSE_EPISODE_TEXT = "n/a";
    public static final String HAVENT_PARSED_EPISODE_TEXT = "---";
    public static final String DEFAULT_SEASON_PREFIX = "Season ";
    public static final String VERSION_SEPARATOR_STRING = "~";

    public static final String ICON_PARENT_DIRECTORY = "res";
    public static final String TVRENAMER_ICON_PATH = "/icons/tvrenamer.png";
    public static final String XML_SUFFIX = ".xml";

    private static final String CONFIGURATION_DIRECTORY_NAME = ".tvrenamer";
    private static final String PREFERENCES_FILENAME = "prefs.xml";
    private static final String OVERRIDES_FILENAME = "overrides.xml";
    private static final String KNOWN_SHOWS_FILENAME = "shows.xml";
    private static final String TVDB_DOWNLOAD_DIRNAME = "thetvdb";
    private static final String TVDB_CACHE_DIRNAME = "cache";

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
    public static final Path THETVDB_DIR
        = CONFIGURATION_DIRECTORY.resolve(TVDB_DOWNLOAD_DIRNAME);
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
