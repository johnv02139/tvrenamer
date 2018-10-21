package org.tvrenamer.controller;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.FileEpisode;

import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilenameParser {
    private static Logger logger = Logger.getLogger(FilenameParser.class.getName());

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
        "(.+?\\W\\D*?)[sS](\\d\\d?)[eE](\\d\\d?).*", // this one matches SXXEXX
        "(.+\\W\\D*?)[sS](\\d\\d?)\\D*?[eE](\\d\\d).*", // this one matches sXX.eXX
        "(.+?\\d{4}\\W\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*", // this one works for titles with years
        "(.+\\W\\D*?)(\\d\\d?)\\D+(\\d\\d).*", // this one matches everything else
        "(.+\\W+)(\\d\\d?)(\\d\\d).*" // truly last resort
    };

    public static final Pattern[] COMPILED_REGEX = new Pattern[REGEX.length];

    static {
        for (int i = 0; i < REGEX.length; i++) {
            COMPILED_REGEX[i] = Pattern.compile(REGEX[i]);
        }
    }

    private FilenameParser() {
        // singleton
    }

    /**
     * Parses the given filename.
     *
     * <p>Given the path associated with the FileEpisode, tries to extract the
     * episode-related information from it.  Uses a hard-coded, ordered list of
     * common patterns that such filenames tend to follow.  As soon as it
     * matches one, it creates a new FileEpisode with that data.
     *
     * @param fileName
     *   the file name we are to try to parse
     * @return
     *   a FileEpisode built from parsing the file name
     */
    public static FileEpisode parseFilename(String fileName) {
        String suffix = StringUtils.getExtension(fileName);

        File f = new File(fileName);
        String fName = stripJunk(insertShowNameIfNeeded(f));
        int idx = 0;
        Matcher matcher;
        while (idx < COMPILED_REGEX.length) {
            // logger.info("trying pattern #" + idx);
            matcher = COMPILED_REGEX[idx++].matcher(fName);
            if (matcher.matches() && matcher.groupCount() == 3) {
                String show = matcher.group(1);
                // logger.info("got hit!  show = " + show);
                show = StringUtils.replacePunctuation(show).toLowerCase();

                int season = Integer.parseInt(matcher.group(2));
                int episode = Integer.parseInt(matcher.group(3));

                FileEpisode ep = new FileEpisode(show, season, episode, suffix, f);
                return ep;
            }
        }

        return new FileEpisode(suffix, f);
    }

    private static String stripJunk(String input) {
        String output = input;
        output = removeLast(output, "hdtv");
        output = removeLast(output, "dvdrip");
        output = removeLast(output, "720p");
        output = removeLast(output, "1080p");
        return output;
    }

    private static String removeLast(String input, String match) {
        int idx = input.toLowerCase().lastIndexOf(match);
        if (idx > 0) {
            input = input.substring(0, idx);
        }
        return input;
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
            logger.finer("appending parent directory '"
                         + parentName + "' to filename '" + fName + "'");
            return parentName + " " + fName;
        } else {
            return fName;
        }
    }
}
