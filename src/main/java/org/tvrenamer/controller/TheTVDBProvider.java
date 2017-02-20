package org.tvrenamer.controller;

import static org.tvrenamer.controller.util.XPathUtilities.nodeListValue;
import static org.tvrenamer.controller.util.XPathUtilities.nodeTextValue;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.Season;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.except.TVRenamerIOException;
import org.tvrenamer.model.util.Constants;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class TheTVDBProvider {
    public static final String IMDB_BASE_URL = "http://www.imdb.com/title/";

    private static final String ERROR_PARSING_XML = "Error parsing XML";
    private static final String ERROR_DOWNLOADING_SHOW_INFORMATION = "Error downloading show information. Check internet or proxy settings";

    private static Logger logger = Logger.getLogger(TheTVDBProvider.class.getName());

    private static final String API_KEY = "4A9560FF0B2670B2";

    private static final String BASE_SEARCH_URL = "http://www.thetvdb.com/api/GetSeries.php?seriesname=";
    private static final String XPATH_SHOW = "/Data/Series";
    private static final String XPATH_SHOWID = "seriesid";
    private static final String XPATH_NAME = "SeriesName";
    private static final String XPATH_IMDB = "IMDB_ID";
    private static final String BASE_LIST_URL = "http://thetvdb.com/api/" + API_KEY + "/series/";
    private static final String BASE_LIST_FILENAME = "/all/en.xml";
    private static final String XPATH_EPISODE_LIST = "/Data/Episode";
    private static final String XPATH_SEASON_NUM = "SeasonNumber";
    private static final String XPATH_EPISODE_NUM = "EpisodeNumber";
    private static final String XPATH_EPISODE_NAME = "EpisodeName";
    private static final String XPATH_AIRDATE = "FirstAired";

    private static final String XPATH_DVD_EPISODE_NUM = "DVD_episodenumber";

    private static final String EPISODE_DATE_FORMAT = "yyyy-MM-dd";

    // Caching
    // This implements a very rudimentary file cache.  Files in the cache never expire.
    // (We never check the file modification date.)  The cache is kept in the config
    // folder of the user's home directory.  I'm sure there are Java libraries
    // out there that could be integrated to provide this functionality, so it's not
    // worth spending much time on, but it also seemed quicker to whip something up on
    // my own than to search for and learn how to use a third-party package.
    //
    // For users, the caching may not even matter.  They'd probably run TVRenamer very
    // infrequently, and when they did re-run it, they'd usually want to have the listings
    // refreshed.  But for developing, it's both slower for me, and more of a strain on
    // TheTVDb.com, to actually fetch the XML from the web every time.
    //
    // Additionally, as a developer, it's actually nice to have the XML there to look at.
    // Although, right now, it's emitted without formatting, which is hard on a human.
    // TODO: if it's easy, would be nice to emit formatted XML.
    // Also TODO: use a real file cache, with eviction, etc.
    private static final Path TvDbCache = Constants.THETVDB_CACHE.toPath();

    static {
        if (Files.notExists(TvDbCache)) {
            try {
                Files.createDirectories(TvDbCache);
            } catch (IOException ioe) {
                logger.info("exception trying to create cache");
            }
        }
    }

    private static File cacheXml(Path path, String xmlText) {
        File cachePath = path.toFile();
        try (PrintWriter pw = new PrintWriter(cachePath)) {
            pw.print(xmlText);
            return cachePath;
        } catch (Exception e) {
            logger.info("caught exception caching xml: " + e);
        }
        return null;
    }

    private static Path seriesOptionsCachePath(String showName) {
        return TvDbCache.resolve(StringUtils.sanitiseTitle(showName) + ".xml");
    }

    private static Path episodeListingsCachePath(Show show) {
        return TvDbCache.resolve(show.getId() + ".xml");
    }

    private static File getShowSearchXml(String showName)
        throws TVRenamerIOException
    {
        Path cachePath = seriesOptionsCachePath(showName);
        if (Files.exists(cachePath)) {
            return cachePath.toFile();
        }
        String searchURL = BASE_SEARCH_URL + StringUtils.encodeSpecialCharacters(showName);

        logger.info("About to download search results from " + searchURL);

        String searchXml = new HttpConnectionHandler().downloadUrl(searchURL);
        return cacheXml(cachePath, searchXml);
    }

    private static File getShowListingXml(Show show)
        throws TVRenamerIOException
    {
        Path cachePath = episodeListingsCachePath(show);
        if (Files.exists(cachePath)) {
            return cachePath.toFile();
        }
        String showURL = BASE_LIST_URL + show.getId() + BASE_LIST_FILENAME;

        logger.info("Downloading episode listing from " + showURL);

        String listingXml = new HttpConnectionHandler().downloadUrl(showURL);
        return cacheXml(cachePath, listingXml);
    }

    private static List<Show> collectShowOptions(NodeList shows, XPath xpath)
        throws XPathExpressionException
    {
        List<Show> options = new ArrayList<>();

        for (int i = 0; i < shows.getLength(); i++) {
            Node eNode = shows.item(i);
            String imdbId = nodeTextValue(XPATH_IMDB, eNode, xpath);
            options.add(new Show(nodeTextValue(XPATH_SHOWID, eNode, xpath),
                                 nodeTextValue(XPATH_NAME, eNode, xpath),
                                 (imdbId == null) ? "" : IMDB_BASE_URL + imdbId));
        }

        return options;
    }

    public static List<Show> getShowOptions(String showName)
        throws TVRenamerIOException
    {
        try {
            File searchXml = getShowSearchXml(showName);

            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new FileReader(searchXml)));

            XPath xpath = XPathFactory.newInstance().newXPath();

            NodeList shows = nodeListValue(XPATH_SHOW, doc, xpath);
            return collectShowOptions(shows, xpath);

        } catch (ConnectException | UnknownHostException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_DOWNLOADING_SHOW_INFORMATION, e);
        } catch (ParserConfigurationException | XPathExpressionException | SAXException | IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }

    private static Season showSeason(Show show, String seasonId) {
        int seasonNum = Integer.parseInt(seasonId);
        Season season = show.getSeason(seasonNum);
        if (season == null) {
            season = new Season(show, seasonNum);
            show.setSeason(seasonNum, season);
        }
        return season;
    }

    private static Integer getEpisodeNumberFromNode(String name, Node eNode, XPath xpath)
        throws XPathExpressionException
    {
        String epNumText = nodeTextValue(name, eNode, xpath);
        if (epNumText != null) {
            try {
                BigDecimal bd = new BigDecimal(epNumText);
                return bd.intValueExact();
            } catch (ArithmeticException | NumberFormatException e) {
                // not an integer
            }
        }
        return null;
    }

    private static Integer getEpisodeNumber(Node eNode, XPath xpath)
        throws XPathExpressionException
    {
        Integer epNum = getEpisodeNumberFromNode(XPATH_DVD_EPISODE_NUM, eNode, xpath);
        if (epNum != null) {
            return epNum;
        }
        return getEpisodeNumberFromNode(XPATH_EPISODE_NUM, eNode, xpath);
    }

    private static Date getEpisodeDate(Node eNode, XPath xpath, DateFormat dateFormatter)
        throws XPathExpressionException
    {
        String airdate = nodeTextValue(XPATH_AIRDATE, eNode, xpath);
        if (StringUtils.isBlank(airdate)) {
            return null;
        }
        try {
            return dateFormatter.parse(airdate);
        } catch (ParseException e) {
            return null;
        }
    }

    private static void addEpisodeToSeason(Node eNode, Show show, XPath xpath,
                                           DateFormat dateFormatter)
    {
        try {
            Integer epNum = getEpisodeNumber(eNode, xpath);
            if (epNum == null) {
                logger.info("ignoring episode with no epnum: " + eNode);
                return;
            }

            String seasonNumString = nodeTextValue(XPATH_SEASON_NUM, eNode, xpath);
            String episodeName = nodeTextValue(XPATH_EPISODE_NAME, eNode, xpath);
            logger.finer("[" + seasonNumString + "x" + epNum + "] " + episodeName);

            Date date = getEpisodeDate(eNode, xpath, dateFormatter);

            Season season = showSeason(show, seasonNumString);
            season.addEpisode(epNum, episodeName, date);
        } catch (Exception e) {
            logger.warning("exception parsing episode of " + show);
            logger.warning(e.toString());
        }
    }

    public static void getShowListing(Show show)
        throws TVRenamerIOException
    {
        try {
            File showXml = getShowListingXml(show);

            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new FileReader(showXml)));

            XPath xpath = XPathFactory.newInstance().newXPath();

            NodeList episodes = nodeListValue(XPATH_EPISODE_LIST, doc, xpath);
            DateFormat dateFormatter = new SimpleDateFormat(EPISODE_DATE_FORMAT);
            for (int i = 0; i < episodes.getLength(); i++) {
                addEpisodeToSeason(episodes.item(i), show, xpath, dateFormatter);
            }
        } catch (ParserConfigurationException | XPathExpressionException | SAXException | IOException | NumberFormatException | DOMException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }
}
