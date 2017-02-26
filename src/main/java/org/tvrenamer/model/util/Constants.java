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
    public static final String TVRENAMER_VERSION_URL = TVRENAMER_PROJECT_URL + "/version";

    public static final String NO_SUCH_SHOW_MESSAGE = "No such show found: ";
    public static final String DOWNLOADING_FAILED_MESSAGE =
            "Downloading show listings failed.  Check internet connection";
    public static final String ADDED_PLACEHOLDER_FILENAME = "Downloading ...";
    public static final String BROKEN_PLACEHOLDER_FILENAME = "Unable to download show information";

    public static final String NO_DND = "Drag and Drop is not currently supported "
        + "on your operating system, please use the 'Browse Files' option above";

    public static final String UNKNOWN_EXCEPTION = "An error occurred, please check "
        + "your internet connection, java version or run from the command line to show errors";
    public static final String ERROR_PARSING_XML = "Error parsing XML";
    public static final String ERROR_DOWNLOADING_SHOW_INFORMATION = "Error downloading "
        + "show information. Check internet or proxy settings";


    public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%0e] %t";
    public static final String DEFAULT_SEASON_PREFIX = "Season ";

    public static final String ICON_PARENT_DIRECTORY = "res";

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
