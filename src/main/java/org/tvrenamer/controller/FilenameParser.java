package org.tvrenamer.controller;

import org.tvrenamer.model.EpisodeInfo;
import org.tvrenamer.model.FileEpisode;

import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilenameParser {
    private static Logger logger = Logger.getLogger(FilenameParser.class.getName());

    public static final String[] REGEX = {
        "(.+?\\d{4}[^a-zA-Z0-9]\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*\\D(\\d+[pk]).*", // this one works for titles with years
        "(.+?[^a-zA-Z0-9]\\D*?)[sS](\\d\\d?)[eE](\\d\\d?).*\\D(\\d+[pk]).*", // this one matches SXXEXX
        "(.+[^a-zA-Z0-9]\\D*?)[sS](\\d\\d?)\\D*?[eE](\\d\\d).*\\D(\\d+[pk]).*", // this one matches sXX.eXX
        "(.+[^a-zA-Z0-9]\\D*?)(\\d\\d?)\\D+(\\d\\d).*\\D(\\d+[pk]).*", // this one matches everything else
        "(.+[^a-zA-Z0-9]+)(\\d\\d?)(\\d\\d).*\\D(\\d+[pk]).*" // truly last resort
    };

    public static final Pattern[] COMPILED_REGEX = new Pattern[REGEX.length * 2];

    static {
        for (int i = 0; i < REGEX.length * 2; i++) {
            if (i / REGEX.length == 0) {
                COMPILED_REGEX[i] = Pattern.compile(REGEX[i]);
            } else {
                COMPILED_REGEX[i] = Pattern.compile(REGEX[i - REGEX.length].replace(".*\\D(\\d+[pk])", ""));
            }
        }
    }

    private FilenameParser() {
        // singleton
    }

    /**
     * Parses the filename of the given FileEpisode.
     *
     * <p>Gets the path associated with the FileEpisode, and tries to extract
     * the episode-related information from it.  Uses a hard-coded, ordered list
     * of common patterns that such filenames tend to follow.  As soon as it
     * matches one, it:<ol>
     *  <li>starts the process of looking up the show name
     *      from the provider, which is done in a separate thread</li>
     *  <li>updates the FileEpisode with the found information</li>
     * </ol>
     *
     * @param episode
     *   the FileEpisode whose filename we are to try to parse
     * @return
     *   true is the filename was successfully parsed, false otherwise
     */
    public static boolean parseFilename(FileEpisode episode) {
        File f = episode.getFile();
        String fName = stripJunk(insertShowNameIfNeeded(f));

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
                episode.setFilenameShow(matcher.group(1));
                episode.setFilenameSeason(Integer.parseInt(matcher.group(2)));
                episode.setFilenameEpisode(Integer.parseInt(matcher.group(3)));
                episode.setFilenameResolution(resolution);
                episode.setStatus(EpisodeInfo.ADDED);

                return true;
            }
        }

        return false;
    }

    private static String stripJunk(String input) {
        String output = input;
        output = removeLast(output, "hdtv");
        output = removeLast(output, "dvdrip");
        return output;
    }

    private static String removeLast(String input, String match) {
        int idx = input.toLowerCase().lastIndexOf(match);
        if (idx > 0) {
            input = input.substring(0, idx) + input.substring(idx + match.length(), input.length());
        }
        return input;
    }

    private static String insertShowNameIfNeeded(File file) {
        String fName = file.getName();
        if (fName.matches("[sS]\\d\\d?[eE]\\d\\d?.*")) {
            String parentName = file.getParentFile().getName();
            if (parentName.toLowerCase().startsWith("season")) {
                parentName = file.getParentFile().getParentFile().getName();
            }
            logger.info("appending parent directory '" + parentName + "' to filename '" + fName + "'");
            return parentName + " " + fName;
        } else {
            return fName;
        }
    }
}
