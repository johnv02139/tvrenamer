package org.tvrenamer.model;

import static org.tvrenamer.controller.util.XPathUtilities.nodeListValue;
import static org.tvrenamer.controller.util.XPathUtilities.nodeTextValue;
import static org.tvrenamer.model.util.Constants.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

/**
 * Represents a TV Series that it is known the user is interested in
 */
public class KnownShow {

    private static Logger logger = Logger.getLogger(KnownShow.class.getName());

    private static List<KnownShow> knownShows = new ArrayList<>();
    private static Map<String, Pattern> patterns = new HashMap<>();

    private static final String XPATH_SHOW = "/Data/Series";
    private static final String XPATH_NAME = "SeriesName";
    private static final String[] SMALL_WORDS = {
        "the", "a", "an", "with", "in", "and", "of", "for", "to", "on"
        // "show"
    };

    static {
        readMyShows();
    }

    private final String showName;
    private final List<String> aliases;
    private String nameKey;
    private String idString;
    private int idNum;

    public KnownShow(String name) {
        showName = name;
        aliases = new ArrayList<>();
    }

    public void addAlias(String alias) {
        aliases.add(alias);
    }

    public String getIdString() {
        return idString;
    }

    public boolean setId(String idString) {
        try {
            idNum = Integer.parseInt(idString);
            this.idString = idString;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("series ID must be an integer, not " + idString);
        }
        return true;
    }

    public String getName() {
        return showName;
    }

    public String getNameKey() {
        return nameKey;
    }

    @Override
    public String toString() {
        return "KnownShow [" + showName + ", id=" + idString + "]";
    }

    private static Pattern createPattern(String line) {
        String pattString = line.toLowerCase();
        int lparen = pattString.lastIndexOf('(');
        if (lparen > 0) {
            pattString = pattString.substring(0, lparen);
        }
        pattString = pattString.replaceAll("'s", "");
        pattString = pattString.replaceAll("[^A-Za-z0-9]+$", "");
        pattString = pattString.replaceAll("[^A-Za-z0-9]+", ".*");
        for (String little : SMALL_WORDS) {
            String lPatt = little.toLowerCase();
            if (pattString.length() < 12) {
                break;
            }
            pattString = pattString.replaceAll("^" + lPatt + "\\.\\*", "");
            pattString = pattString.replaceAll("\\.\\*" + lPatt + "\\.\\*", ".*");
        }
        pattString = ".*(" + pattString + ").*";

        return Pattern.compile(pattString, Pattern.CASE_INSENSITIVE);
    }

    private static boolean collectShowPatterns(NodeList shows) {

        try {
            for (int i = 0; i < shows.getLength(); i++) {
                // logger.info("collectShowPatterns: " + i);
                Node eNode = shows.item(i);
                String seriesName = nodeTextValue(XPATH_NAME, eNode);
                // logger.info("seriesName: " + seriesName);
                // TODO: aliases

                KnownShow series = new KnownShow(seriesName);
                knownShows.add(series);
                patterns.put(seriesName, createPattern(seriesName));
            }
        } catch (XPathExpressionException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        }

        return true;
    }

    public static boolean readMyShows() {
        File myShows = KNOWN_SHOWS_FILE.toFile();
        if (!myShows.exists()) {
            logger.info("no list of known shows found");
            return false;
        }
        NodeList shows = null;

        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new FileReader(myShows)));

            shows = nodeListValue(XPATH_SHOW, doc);
            // logger.info("got " + shows.getLength() + " shows");
        } catch (ParserConfigurationException | IOException | XPathExpressionException | SAXException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        }
        return collectShowPatterns(shows);
    }

    public static String mapFilenameToShow(String filename) {
        List<String> allMatches = new ArrayList<>();
        Set<String> shows = patterns.keySet();
        for (String show : shows) {
            Pattern patt = patterns.get(show);
            Matcher matcher = patt.matcher(filename);
            if (matcher.matches()) {
                allMatches.add(show);
            }
        }

        int count = allMatches.size();
        if (count == 0) {
            return null;
        }

        if (count == 1) {
            return allMatches.get(0);
        }

        logger.warning("matched multiple: " + filename);
        for (String showName : allMatches) {
            logger.warning("    " + showName);
        }
        // TODO: if we had more than one match, we could choose, somehow.
        // But for now, just give up, and rely on FilenameParser to do better.
        return null;
    }

    public static void writeSeries(Writer writer, String showName) {
        try {
            writer.write("  <Series>\n");
            writer.write("    <Name>");
            writer.write(showName);
            writer.write("</Name>\n");
            writer.write("  </Series>\n");
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "error writing series xml", ioe);
        }
    }

    public static void writeShows() {
        String outFileName = "c:/Users/jv/Documents/VC/tvrenamer/etc/myshows.xml";
        Path outPath = Paths.get(outFileName);
        try (Writer writer = Files.newBufferedWriter(outPath)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            writer.write("<Data>\n");

            for (KnownShow show : knownShows) {
                writeSeries(writer, show.getName());
            }
            for (KnownShow show : knownShows) {
                String showName = show.getName();
                Pattern patt = patterns.get(showName);
                Matcher matcher;
                List<String> hits = new ArrayList<>();
                for (KnownShow show2 : knownShows) {
                    String show2Name = show2.getName();
                    matcher = patt.matcher(show2Name);
                    if (matcher.matches()) {
                        hits.add(show2Name + "(" + matcher.group(1) + ")");
                    }
                }
                if (hits.size() != 1) {
                    logger.warning("** " + patt);
                    for (String show3 : hits) {
                        logger.warning("-- " + show3);
                    }
                }
            }
            writer.write("</Data>\n");
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "error writing file", ioe);
        }
    }
}
