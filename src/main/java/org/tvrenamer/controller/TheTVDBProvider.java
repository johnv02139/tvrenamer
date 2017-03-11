package org.tvrenamer.controller;

import static org.tvrenamer.controller.util.XPathUtilities.nodeListValue;
import static org.tvrenamer.controller.util.XPathUtilities.nodeTextValue;
import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.Episode;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.except.TVRenamerIOException;

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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class TheTVDBProvider {
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
    private static final String XPATH_OVERVIEW = "Overview";
    private static final String XPATH_PRODUCTION_CODE = "ProductionCode";
    private static final String XPATH_LANGUAGE = "Language";
    private static final String XPATH_EPISODE_ID = "id";
    private static final String XPATH_SERIES_ID = "seriesid";
    private static final String XPATH_SEASON_ID = "seasonid";
    private static final String XPATH_LAST_UPDATE = "lastupdated";
    private static final String XPATH_DVD_SEASON = "DVD_season";
    private static final String XPATH_DVD_EPISODE_NUM = "DVD_episodenumber";

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
    private static final Path TvDbCache = THETVDB_DIR;

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

    private static Path episodeListingsCachePath(String seriesId) {
        return TvDbCache.resolve(seriesId + ".xml");
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

    private static File getXmlListings(String seriesId)
        throws TVRenamerIOException
    {
        Path cachePath = episodeListingsCachePath(seriesId);
        if (Files.exists(cachePath)) {
            return cachePath.toFile();
        }
        String showURL = BASE_LIST_URL + seriesId + BASE_LIST_FILENAME;

        logger.info("Downloading episode listing from " + showURL);

        String listingXml = new HttpConnectionHandler().downloadUrl(showURL);
        return cacheXml(cachePath, listingXml);
    }

    private static List<Series> collectShowOptions(NodeList shows)
        throws XPathExpressionException
    {
        List<Series> options = new ArrayList<>();

        for (int i = 0; i < shows.getLength(); i++) {
            Node eNode = shows.item(i);
            String seriesName = nodeTextValue(XPATH_NAME, eNode);
            String tvdbId = nodeTextValue(XPATH_SHOWID, eNode);
            String imdbId = nodeTextValue(XPATH_IMDB, eNode);

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

            NodeList shows = nodeListValue(XPATH_SHOW, doc);
            return collectShowOptions(shows);

        } catch (ConnectException | UnknownHostException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_DOWNLOADING_SHOW_INFORMATION, e);
        } catch (ParserConfigurationException | XPathExpressionException | SAXException | IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }

    private static NodeList getEpisodeList(String seriesId)
        throws TVRenamerIOException
    {
        NodeList episodeList;

        DocumentBuilder bld;
        try {
            bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.log(Level.WARNING, "could not create DocumentBuilder: " + e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }

        try {
            File listingsXml = getXmlListings(seriesId);
            Document doc = bld.parse(new InputSource(new FileReader(listingsXml)));
            episodeList = nodeListValue(XPATH_EPISODE_LIST, doc);
        } catch (XPathExpressionException | SAXException | IOException | NumberFormatException | DOMException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
        return episodeList;
    }

    private static Episode createEpisode(Node eNode)
        throws XPathExpressionException
    {
        return new Episode.Builder()
            .seasonNum(nodeTextValue(XPATH_SEASON_NUM, eNode))
            .episodeNum(nodeTextValue(XPATH_EPISODE_NUM, eNode))
            .title(nodeTextValue(XPATH_EPISODE_NAME, eNode))
            .airDate(nodeTextValue(XPATH_AIRDATE, eNode))
            .build();
    }

    private static Episode parseError(int i, Exception e, String seriesName) {
        String msg = "exception parsing episode " + i;
        if (seriesName != null) {
            msg += " of " + seriesName;
        }
        logger.warning(msg);
        logger.warning(e.toString());

        return null;
    }

    public static Episode[] getListings(String seriesId, String seriesName)
        throws TVRenamerIOException
    {
        NodeList episodeList = getEpisodeList(seriesId);

        int episodeCount = episodeList.getLength();
        Episode[] episodes = new Episode[episodeCount];
        for (int i = 0; i < episodeCount; i++) {
            try {
                episodes[i] = createEpisode(episodeList.item(i));
            } catch (Exception e) {
                episodes[i] = parseError(i, e, seriesName);
            }
        }
        return episodes;
    }

    public static Episode[] getListings(String seriesId)
        throws TVRenamerIOException
    {
        return getListings(seriesId, null);
    }
}
