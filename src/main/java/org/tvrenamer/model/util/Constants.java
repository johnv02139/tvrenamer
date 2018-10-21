package org.tvrenamer.model.util;

import java.io.File;
import java.util.Locale;

public class Constants {

    public static final Locale THIS_LOCALE = Locale.getDefault();

    public static final String APPLICATION_NAME = "TV Show Finder";

    public static final String VERSION_NUMBER = Environment.readVersionNumber();

    public static final String SHOWFINDER_PROJECT_URL = "https://github.com/eprenamer/tvrenamer/tree/showfinder";
    public static final String SHOWFINDER_DOWNLOAD_URL = SHOWFINDER_PROJECT_URL + "/downloads";

    public static final String NO_SUCH_SHOW_MESSAGE = "No such show found: ";
    public static final String DOWNLOADING_FAILED_MESSAGE =
            "Downloading show listings failed.  Check internet connection";
    public static final String ADDED_PLACEHOLDER_FILENAME = "Downloading ...";
    public static final String BROKEN_PLACEHOLDER_FILENAME = "Unable to download show information";

    public static final String NO_DND = "Drag and Drop is not currently supported "
        + "on your operating system, please use the 'Browse Files' option above";

    public static final String UNKNOWN_EXCEPTION = "An error occurred, please check "
        + "your internet connection, java version or run from the command line to show errors";

    public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%0e] %t";
    public static final String DEFAULT_SEASON_PREFIX = "Season ";

    public static final String ICON_PARENT_DIRECTORY = "res";

    private static final String CONFIGURATION_DIRECTORY_NAME = ".showfinder";
    private static final String PREFERENCES_FILENAME = "prefs.xml";
    private static final String OVERRIDES_FILENAME = "overrides.xml";
    private static final String KNOWN_SHOWS_FILENAME = "shows.xml";
    private static final String TVDB_CACHE_DIRNAME = "thetvdb";

    public static final File USER_HOME_DIR = new File(Environment.USER_HOME);

    public static final File DEFAULT_DESTINATION_DIRECTORY
        = new File(USER_HOME_DIR, "TV");
    public static final File CONFIGURATION_DIRECTORY
        = new File(USER_HOME_DIR, CONFIGURATION_DIRECTORY_NAME);
    public static final File PREFERENCES_FILE
        = new File(CONFIGURATION_DIRECTORY, PREFERENCES_FILENAME);
    public static final File OVERRIDES_FILE
        = new File(CONFIGURATION_DIRECTORY, OVERRIDES_FILENAME);
    public static final File THETVDB_CACHE
        = new File(CONFIGURATION_DIRECTORY, TVDB_CACHE_DIRNAME);
    public static final File KNOWN_SHOWS_FILE
        = new File(CONFIGURATION_DIRECTORY, KNOWN_SHOWS_FILENAME);

    public static final File PREFERENCES_FILE_LEGACY
        = new File(USER_HOME_DIR, "tvrenamer.preferences");
    public static final File OVERRIDES_FILE_LEGACY
        = new File(USER_HOME_DIR, ".tvrenameroverrides");

}
