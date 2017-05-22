package org.tvrenamer.controller;

import static org.tvrenamer.controller.util.XPathUtilities.nodeListValue;
import static org.tvrenamer.controller.util.XPathUtilities.nodeTextValue;
import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.model.EpisodeInfo;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowName;
import org.tvrenamer.model.TVRenamerIOException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;

public class TheTVDBProvider {
    private static final Logger logger = Logger.getLogger(TheTVDBProvider.class.getName());

    // The unique API key for our application
    private static final String API_KEY = "4A9560FF0B2670B2";

    // The proposed day on which the v1 API will cease to be supported.
    private static final LocalDate SUNSET = LocalDate.of(2017, Month.OCTOBER, 1);

    // Whether or not we should try making v1 API calls
    private static boolean apiIsDeprecated = false;

    // The base information for the provider
    private static final String DEFAULT_SITE_URL = "http://thetvdb.com/";
    private static final String API_URL = DEFAULT_SITE_URL + "api/";

    // The URL to get, to receive options for a given series search string.
    // Note, does not take API key.
    private static final String BASE_SEARCH_URL = API_URL + "GetSeries.php?seriesname=";

    // These are the tags that we use to extract the relevant information from the show document.
    private static final String XPATH_SHOW = "/Data/Series";
    private static final String XPATH_SHOWID = "seriesid";
    private static final String XPATH_NAME = "SeriesName";
    private static final String SERIES_NOT_PERMITTED = "** 403: Series Not Permitted **";

    // The URL to get, to receive listings for a specific given series.
    private static final String BASE_LIST_URL = API_URL + API_KEY + "/series/";
    private static final String BASE_LIST_FILENAME = "/all/" + DEFAULT_LANGUAGE + XML_SUFFIX;

    // These are the tags that we use to extract the episode information
    // from the listings document.
    private static final String XPATH_EPISODE_LIST = "/Data/Episode";
    private static final String XPATH_EPISODE_ID = "id";
    private static final String XPATH_SEASON_NUM = "SeasonNumber";
    private static final String XPATH_EPISODE_NUM = "EpisodeNumber";
    private static final String XPATH_EPISODE_NAME = "EpisodeName";
    private static final String XPATH_AIRDATE = "FirstAired";
    private static final String XPATH_EPISODE_SERIES_ID = "seriesid";
    private static final String XPATH_DVD_SEASON_NUM = "DVD_season";
    private static final String XPATH_DVD_EPISODE_NUM = "DVD_episodenumber";
    private static final String XPATH_EPISODE_NUM_ABS = "absolute_number";

    private static final TransformerFactory TFACTORY = TransformerFactory.newInstance();

    private static DocumentBuilder createDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            String msg = "could not create DocumentBuilder";
            logger.warning(msg + ": " + e.getMessage());
            // There is truly no way to continue without a DocumentBuilder.
            // Throw an exception that we assume will not be caught until
            // the top-level loop, causing the program to exit.
            throw new IllegalStateException(msg);
        }
    }

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

    /**
     * Store the given XML text in our cache, at the specified location.
     *
     * @param path
     *    the location to store the XML text
     * @param document
     *    the DOM document to write out to the file
     * @return a Path
     *    the file that was created with the XML text
     */
    private static Path cacheXml(final Path path, final Document document)
        throws TVRenamerIOException
    {
        if (document == null) {
            return null;
        }
        // Use a Transformer for output
        try {
            Transformer transformer = TFACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(Files.newOutputStream(path));
            transformer.transform(source, result);
        } catch (TransformerException e) {
            logger.warning("unable to transform document: " + e.getMessage());
        } catch (IOException e) {
            logger.warning("unable to write out document: " + e.getMessage());
        }

        return null;
    }

    /**
     * Delete the given path.  Actually a very generic method, but intended
     * only to delete a cached XML file.
     *
     * @param path
     *   The file to be deleted
     * @return true if the path was deleted; false if it was not deleted
     *   (including if it did not exist)
     */
    private static boolean decacheXml(final Path path) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                logger.warning("cannot decache directory: " + path);
                return false;
            }
            try {
                // There can be several reasons for wanting to decache the file.
                // In some cases, we do it because the file is bad.  In that case,
                // we want the file out of the way, but we might actually prefer
                // to put it aside somewhere, so it can be inspected later.  But
                // for now, just delete it.  (TODO)
                Files.delete(path);
            } catch (IOException ioe) {
                return false;
            }
            if (Files.notExists(path)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isApiDiscontinuedError(Throwable e) {
        if (0 > LocalDate.now().compareTo(SUNSET)) {
            return false;
        }
        while (e != null) {
            if (e instanceof FileNotFoundException) {
                apiIsDeprecated = true;
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    public static boolean isApiDeprecated() {
        return apiIsDeprecated;
    }

    /**
     * Read XML to produce a DOM Document.  This is just a wrapper around
     * DocumentBuilder.parse() that handles any exceptions.
     *
     * @param xmlSource
     *   The text of an XML document to be parsed.  The content doesn't matter.
     * @param sourceMsg
     *   A piece of text describing the XML we're parsing.  Only used in the
     *   case of an exception, to report back which XML had a problem.
     * @return a Document that is the result of parsing the given XML
     */
    private static Document readDocumentFromInputSource(final InputSource xmlSource,
                                                        final String sourceMsg)
    {
        final DocumentBuilder documentBuilder = createDocumentBuilder();

        try {
            Document doc = documentBuilder.parse(xmlSource);
            return doc;
        } catch (SAXException | IOException e) {
            // logger.log(Level.WARNING, ERROR_PARSING_XML + sourceMsg, e);
            // We really don't need to see the stacktrace, so instead of using
            // logger.log with the exception, just log the message.
            logger.warning(ERROR_PARSING_XML + sourceMsg + ": "
                           + e.getMessage());
        }
        return null;
    }

    /**
     * Create and return the Document of the options for the given show name.
     *
     * @param showName
     *    the ShowName to look up options for
     * @return a Document with all the options for the show name
     */
    private static Document buildShowSearchDocument(final ShowName showName)
        throws TVRenamerIOException
    {
        Document doc = null;
        InputSource input = null;

        Path cachePath = THETVDB_DL_DIR.resolve(showName.getSanitised() + XML_SUFFIX);
        if (Files.notExists(cachePath)) {
            String searchURL = BASE_SEARCH_URL + showName.getQueryString();
            logger.info("About to download search results from " + searchURL);

            String xmlText = new HttpConnectionHandler().downloadUrl(searchURL);
            input = new InputSource(new StringReader(xmlText));
            doc = readDocumentFromInputSource(input, " from show query download for "
                                              + showName);
            cacheXml(cachePath, doc);
        } else {
            try (BufferedReader reader = Files.newBufferedReader(cachePath, TVDB_CHARSET)) {
                input = new InputSource(reader);
                doc = readDocumentFromInputSource(input, " while reading from " + cachePath);
            } catch (IOException e) {
                decacheXml(cachePath);
                logger.warning(ERROR_PARSING_XML + "while creating reader for " + cachePath);
                throw new TVRenamerIOException(ERROR_PARSING_XML, e);
            }
        }

        return doc;
    }

    private static String getShowSearchXml(final ShowName showName)
        throws TVRenamerIOException
    {
        if (apiIsDeprecated) {
            throw new TVRenamerIOException(API_DISCONTINUED, null);
        }

        String searchURL = BASE_SEARCH_URL + showName.getQueryString();

        logger.fine("About to download search results from " + searchURL);

        //noinspection UnnecessaryLocalVariable
        String searchXmlText = new HttpConnectionHandler().downloadUrl(searchURL);
        return searchXmlText;
    }

    private static String getShowListingXml(final Show show)
        throws TVRenamerIOException
    {
        if (apiIsDeprecated) {
            throw new TVRenamerIOException(API_DISCONTINUED, null);
        }

        Integer showId = show.getId();
        if (showId == null) {
            throw new TVRenamerIOException("cannot download listings for show "
                                           + show.getName()
                                           + " because it has no integer ID");
        }
        String showURL = BASE_LIST_URL + showId + BASE_LIST_FILENAME;

        logger.fine("Downloading episode listing from " + showURL);

        //noinspection UnnecessaryLocalVariable
        String listingXmlText = new HttpConnectionHandler().downloadUrl(showURL);
        return listingXmlText;
    }

    private static void collectShowOptions(final NodeList shows, final ShowName showName)
        throws XPathExpressionException
    {
        for (int i = 0; i < shows.getLength(); i++) {
            Node eNode = shows.item(i);
            String seriesName = nodeTextValue(XPATH_NAME, eNode);
            String tvdbId = nodeTextValue(XPATH_SHOWID, eNode);

            if (SERIES_NOT_PERMITTED.equals(seriesName)) {
                logger.warning("ignoring unpermitted option for "
                               + showName.getFoundName());
            } else {
                showName.addShowOption(tvdbId, seriesName);
            }
        }
    }

    private static void readShowsFromInputSource(final DocumentBuilder bld,
                                                 final InputSource searchXmlSource,
                                                 final ShowName showName)
        throws TVRenamerIOException
    {
        try {
            Document doc = bld.parse(searchXmlSource);
            NodeList shows = nodeListValue(XPATH_SHOW, doc);
            collectShowOptions(shows, showName);
        } catch (SAXException | XPathExpressionException | DOMException | IOException e) {
            logger.log(Level.WARNING, ERROR_PARSING_XML, e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }

    /**
     * Get the potential options for the given ShowName.  Does not return a value,
     * nor does it use a listener-style interface.  The ShowName object serves the
     * purpose both of providing information about what we're looking up, and being
     * the receptacle for the result.
     *
     * @param showName
     *    the ShowName object telling us information about the show name we should
     *    query for, and also the object that we should provide the options to
     */
    public static void getShowOptions(final ShowName showName)
        throws TVRenamerIOException
    {
        if (apiIsDeprecated) {
            throw new TVRenamerIOException(API_DISCONTINUED, null);
        }

        try {
            Document doc = buildShowSearchDocument(showName);
            if (doc == null) {
                logger.warning("unable to get options for " + showName);
                return;
            }

            NodeList shows = nodeListValue(XPATH_SHOW, doc);
            for (int i = 0; i < shows.getLength(); i++) {
                Node eNode = shows.item(i);
                String seriesName = nodeTextValue(XPATH_NAME, eNode);
                if (SERIES_NOT_PERMITTED.equals(seriesName)) {
                    logger.warning("ignoring unpermitted option for "
                                   + showName.getFoundName());
                } else {
                    showName.addShowOption(nodeTextValue(XPATH_SHOWID, eNode),
                                           seriesName);
                }
            }
        } catch (TVRenamerIOException tve) {
            if (isApiDiscontinuedError(tve)) {
                throw new TVRenamerIOException(API_DISCONTINUED, tve);
            } else {
                throw tve;
            }
        } catch (XPathExpressionException e) {
            logger.warning(ERROR_PARSING_XML + ": " + showName + "; "
                           + e.getMessage());
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }

    /**
     * Create and return the Document of the listings for the given show.
     *
     * @param show
     *    the Show to look up listings for
     * @return a Document with all the listing of all the show's episodes
     */
    private static Document getListingsDocument(final Show show)
        throws TVRenamerIOException
    {
        Document doc = null;
        String showIdString = String.valueOf(show.getId());
        Path cachePath = THETVDB_DL_DIR.resolve(showIdString + XML_SUFFIX);
        if (Files.notExists(cachePath)) {
            String showURL = BASE_LIST_URL + showIdString + BASE_LIST_FILENAME;
            logger.info("Downloading episode listing from " + showURL);

            try {
                String xmlText = new HttpConnectionHandler().downloadUrl(showURL);
                InputSource input = new InputSource(new StringReader(xmlText));
                doc = readDocumentFromInputSource(input, " from listings download for"
                                                  + show.getName());
                cacheXml(cachePath, doc);
            } catch (IOException e) {
                logger.warning(ERROR_PARSING_XML);
                throw new TVRenamerIOException(ERROR_PARSING_XML, e);
            }
        } else {
            try (BufferedReader reader = Files.newBufferedReader(cachePath, TVDB_CHARSET))  {
                InputSource input = new InputSource(reader);
                doc = readDocumentFromInputSource(input, " while reading listings for "
                                                  + show.getName() + " from " + cachePath);
            } catch (IOException e) {
                decacheXml(cachePath);
                logger.warning(ERROR_PARSING_XML);
                throw new TVRenamerIOException(ERROR_PARSING_XML, e);
            }
        }

        return doc;
    }

    /**
     * Transform a DOM node with episode information, into an EpisodeInfo object.
     *
     * @param eNode
     *    the DOM object containing all the information about the episode, derived
     *    from the downloaded XML
     * @return an EpisodeInfo object with the selected information
     */
    private static EpisodeInfo createEpisodeInfo(final Node eNode) {
        try {
            return new EpisodeInfo.Builder()
                .episodeId(nodeTextValue(XPATH_EPISODE_ID, eNode))
                .seasonNumber(nodeTextValue(XPATH_SEASON_NUM, eNode))
                .episodeNumber(nodeTextValue(XPATH_EPISODE_NUM, eNode))
                .episodeName(nodeTextValue(XPATH_EPISODE_NAME, eNode))
                .firstAired(nodeTextValue(XPATH_AIRDATE, eNode))
                .dvdSeason(nodeTextValue(XPATH_DVD_SEASON_NUM, eNode))
                .dvdEpisodeNumber(nodeTextValue(XPATH_DVD_EPISODE_NUM, eNode))
                .absoluteNumber(nodeTextValue(XPATH_EPISODE_NUM_ABS, eNode))
                .seriesId(nodeTextValue(XPATH_EPISODE_SERIES_ID, eNode))
                .build();
        } catch (Exception e) {
            logger.log(Level.WARNING, "exception parsing episode", e);
        }
        return null;
    }

    /**
     * Get the episode listings for the given Show.  Does not return a value, nor
     * does it use a listener-style interface.  The Show object serves the purpose
     * both of providing information about what we're looking up, and being the
     * receptacle for the result.
     *
     * @param show
     *    the Show object telling us information about the show we should get the
     *    listings for, and also the object that we should provide the result to
     */
    private static void getShowEpisodes(final Show show)
        throws TVRenamerIOException
    {
        NodeList episodeList;
        try {
            Document doc = getListingsDocument(show);
            if (doc == null) {
                return;
            }
            episodeList = nodeListValue(XPATH_EPISODE_LIST, doc);
        } catch (XPathExpressionException e) {
            logger.warning(e.getMessage());
            throw new TVRenamerIOException(ERROR_PARSING_XML + ": " + show, e);
        }

        int episodeCount = episodeList.getLength();
        EpisodeInfo[] episodeInfos = new EpisodeInfo[episodeCount];
        for (int i = 0; i < episodeCount; i++) {
            episodeInfos[i] = createEpisodeInfo(episodeList.item(i));
        }
        show.addEpisodes(episodeInfos);
    }

    /**
     * Get the episode listings for the given Show.  Does not return a value, nor
     * does it use a listener-style interface.  The Show object serves the purpose
     * both of providing information about what we're looking up, and being the
     * receptacle for the result.
     *
     * @param show
     *    the Show object telling us information about the show we should get the
     *    listings for, and also the object that we should provide the result to
     */
    public static void getShowListing(final Show show)
        throws TVRenamerIOException
    {
        if (apiIsDeprecated) {
            throw new TVRenamerIOException(API_DISCONTINUED, null);
        }

        Integer showId = show.getId();
        if (showId == null) {
            logger.warning("cannot download listings for show "
                           + show.getName()
                           + " because it has no integer ID");
        }
        if (showId <= 0) {
            logger.warning("cannot download listings for show "
                           + show.getName()
                           + " because it has no positive ID");
        }

        getShowEpisodes(show);
    }
}
