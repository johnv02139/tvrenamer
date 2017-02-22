package org.tvrenamer.controller;

import static org.tvrenamer.controller.util.XPathUtilities.nodeListValue;
import static org.tvrenamer.controller.util.XPathUtilities.nodeTextValue;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.Episode;
import org.tvrenamer.model.Series;
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
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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

    private static Path episodeListingsCachePath(Series series) {
        return TvDbCache.resolve(series.getIdString() + ".xml");
    }

    private static File performShowQuery(String showName)
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

    private static File getXmlListings(Series series)
        throws TVRenamerIOException
    {
        Path cachePath = episodeListingsCachePath(series);
        if (Files.exists(cachePath)) {
            return cachePath.toFile();
        }
        String showURL = BASE_LIST_URL + series.getIdString() + BASE_LIST_FILENAME;

        logger.info("Downloading episode listing from " + showURL);

        String listingXml = new HttpConnectionHandler().downloadUrl(showURL);
        return cacheXml(cachePath, listingXml);
    }

    private static List<Series> collectShowOptions(NodeList shows, XPath xpath)
        throws XPathExpressionException
    {
        List<Series> options = new ArrayList<>();

        for (int i = 0; i < shows.getLength(); i++) {
            Node eNode = shows.item(i);
            String seriesName = nodeTextValue(XPATH_NAME, eNode, xpath);
            String tvdbId = nodeTextValue(XPATH_SHOWID, eNode, xpath);
            String imdbId = nodeTextValue(XPATH_IMDB, eNode, xpath);

            Series series = new Series(seriesName, tvdbId, imdbId);
            options.add(series);
        }

        return options;
    }

    public static List<Series> querySeriesName(String showName)
        throws TVRenamerIOException
    {
        try {
            File searchXml = performShowQuery(showName);

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

    private static Integer getEpisodeNumber(Node eNode, XPath xpath)
        throws XPathExpressionException
    {
        String epNumText = nodeTextValue(XPATH_EPISODE_NUM, eNode, xpath);
        Integer epNum = StringUtils.stringToInt(epNumText);
        if (epNum != null) {
            return epNum;
        }
        // Did not find "regular" episode number, try DVD episode number
        epNumText = nodeTextValue(XPATH_DVD_EPISODE_NUM, eNode, xpath);
        return StringUtils.stringToInt(epNumText);
    }

    private static LocalDate getEpisodeDate(Node eNode, XPath xpath, DateTimeFormatter dateFormatter)
        throws XPathExpressionException
    {
        String airdate = nodeTextValue(XPATH_AIRDATE, eNode, xpath);
        if (StringUtils.isBlank(airdate)) {
            return null;
        }
        try {
            return (LocalDate) dateFormatter.parse(airdate, LocalDate::from);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static void addEpisodeToSeries(Node eNode, Series series, XPath xpath,
                                           DateTimeFormatter dateFormatter)
    {
        try {
            Integer epNum = getEpisodeNumber(eNode, xpath);
            if (epNum == null) {
                logger.info("ignoring episode with no epnum: " + eNode);
                return;
            }

            String seasonNumString = nodeTextValue(XPATH_SEASON_NUM, eNode, xpath);
            String episodeName = nodeTextValue(XPATH_EPISODE_NAME, eNode, xpath);
            logger.finer("[S" + seasonNumString + "E" + epNum + "] "
                         + episodeName);

            LocalDate date = getEpisodeDate(eNode, xpath, dateFormatter);

            Episode ep = new Episode.Builder()
                .episodeNum(epNum)
                .title(episodeName)
                .airDate(date)
                .build();

            series.addEpisode(ep, seasonNumString, epNum);
        } catch (Exception e) {
            logger.warning("exception parsing episode of " + series);
            logger.warning(e.toString());
        }
    }

    public static void getListings(Series series)
        throws TVRenamerIOException
    {
        try {
            File listingsXml = getXmlListings(series);

            DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = bld.parse(new InputSource(new FileReader(listingsXml)));

            XPath xpath = XPathFactory.newInstance().newXPath();

            NodeList episodes = nodeListValue(XPATH_EPISODE_LIST, doc, xpath);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(EPISODE_DATE_FORMAT);
            for (int i = 0; i < episodes.getLength(); i++) {
                addEpisodeToSeries(episodes.item(i), series, xpath, dateFormatter);
            }
        } catch (ParserConfigurationException | XPathExpressionException | SAXException | IOException | NumberFormatException | DOMException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }
}
