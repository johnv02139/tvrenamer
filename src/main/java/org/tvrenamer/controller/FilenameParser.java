package org.tvrenamer.controller;

import org.tvrenamer.model.FileEpisode;

import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilenameParser {
    private static Logger logger = Logger.getLogger(FilenameParser.class.getName());

    private static final String RESOLUTION_REGEX = "\\D(\\d+[pk]).*";

    // These regular expressions are used in sequence, until one matches.  Once we hit a match,
    // we stop.  The original ordering had "titles with years" first, but I found that many
    // files had both an episode number and a date, and usually the episode number came first.
    // So, for example, if we had:
    //    "My Show - S01E10 - Episode - 12/31/1999.avi"
    // ... we'd look for "My Show - S01E10 - Episode" as the show name, instead of just "My Show".
    // I think the right thing to do is successively apply all of them, basically stripping away
    // all non-show-title information.  For now, the simplest thing for me is just changing the
    // ordering so that we truncate at the episode number first.
    public static final String[] REGEX = {
            "(.+?[^a-zA-Z0-9]\\D*?)[sS](\\d\\d?)[eE](\\d\\d?).*", // this one matches SXXEXX
            "(.+[^a-zA-Z0-9]\\D*?)[sS](\\d\\d?)\\D*?[eE](\\d\\d).*", // this one matches sXX.eXX
            "(.+?\\d{4}[^a-zA-Z0-9]\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*", // this one works for titles with years
            "(.+[^a-zA-Z0-9]\\D*?)(\\d\\d?)\\D+(\\d\\d).*", // this one matches everything else
            "(.+[^a-zA-Z0-9]+)(\\d\\d?)(\\d\\d).*" // truly last resort
    };

    public static final Pattern[] COMPILED_REGEX = new Pattern[REGEX.length * 2];

    static {
        for (int i = 0; i < REGEX.length; i++) {
            COMPILED_REGEX[i] = Pattern.compile(REGEX[i] + RESOLUTION_REGEX);
        }
        for (int i = 0; i < REGEX.length; i++) {
            COMPILED_REGEX[i + REGEX.length] = Pattern.compile(REGEX[i]);
        }
    }

    private static String removeLast(String input, String match) {
        int idx = input.toLowerCase().lastIndexOf(match);
        if (idx > 0) {
            input = input.substring(0, idx) + input.substring(idx + match.length(), input.length());
        }
        return input;
    }

    private static String stripJunk(String input) {
        String output = input;
        output = removeLast(output, "hdtv");
        output = removeLast(output, "dvdrip");
        return output;
    }

    private static String insertShowNameIfNeeded(File file) {
        String fName = file.getName();
        // TODO: don't inline these patterns; can we use same ones
        // as used by UserPreferences?
        if (fName.matches("[sS]\\d\\d?[eE]\\d\\d?.*")) {
            String parentName = file.getParentFile().getName();
            if (parentName.toLowerCase().startsWith("season")
                || parentName.matches("[sS][0-3][0-9]"))
            {
                parentName = file.getParentFile().getParentFile().getName();
            }
            logger.fine("appending parent directory '" + parentName + "' to filename '" + fName + "'");
            return parentName + " " + fName;
        } else {
            return fName;
        }
    }

    public static boolean parseFilename(FileEpisode episode) {
        File f = episode.getFile();
        String withShow = insertShowNameIfNeeded(f);
        String fName = stripJunk(withShow);

        int idx = 0;
        Matcher matcher;
        while (idx < COMPILED_REGEX.length) {
            matcher = COMPILED_REGEX[idx++].matcher(fName);
            if (matcher.matches()) {
                String resolution = "";
                if (matcher.groupCount() == 4) {
                    resolution = matcher.group(4);
                } else if (matcher.groupCount() != 3) {
                    // This should never happen and so we should probably consider it
                    // an error if it does, but not important.
                    continue;
                }
                episode.setRawSeries(matcher.group(1));
                episode.setFilenameSeason(matcher.group(2));
                episode.setFilenameEpisode(matcher.group(3));
                episode.setFilenameResolution(resolution);
                episode.setParsed();
                return true;
            }
        }
        episode.setBadParse();
        return false;
    }

    // prevent instantiation
    private FilenameParser() { }
}
