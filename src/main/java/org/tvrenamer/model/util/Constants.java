package org.tvrenamer.model.util;

import java.io.File;

public class Constants {

    public static final String APPLICATION_NAME = "TVRenamer";

    public static final String VERSION_NUMBER = Environment.readVersionNumber();

    public static final File USER_HOME_DIR = new File(Environment.USER_HOME);

    public static final String PREFERENCES_FILE = ".tvrenamer";

    public static final String PREFERENCES_FILE_LEGACY = "tvrenamer.preferences";

    public static final String OVERRIDES_FILE = ".tvrenameroverrides";

    public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%0e] %t";

    public static final File DEFAULT_DESTINATION_DIRECTORY = new File(USER_HOME_DIR, "TV");

    public static final String DEFAULT_SEASON_PREFIX = "Season ";
}
