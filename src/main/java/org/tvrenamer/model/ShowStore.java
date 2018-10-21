package org.tvrenamer.model;

import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.TheTVDBProvider;
import org.tvrenamer.controller.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ShowStore {

    private static Logger logger = Logger.getLogger(ShowStore.class.getName());

    private static class ShowRegistrations {
        private final List<ShowInformationListener> mListeners;

        public ShowRegistrations() {
            this.mListeners = new LinkedList<>();
        }

        public void addListener(ShowInformationListener listener) {
            this.mListeners.add(listener);
        }

        public List<ShowInformationListener> getListeners() {
            return Collections.unmodifiableList(mListeners);
        }
    }

    private static final Map<String, List<Show>> _shows = new ConcurrentHashMap<>(100);

    private static final Map<String, ShowRegistrations> _showRegistrations
        = new ConcurrentHashMap<>();

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * Transform a string which we believe represents a show name, to the string we will
     * use for the query.
     *
     * For the internal data structures used by this class, the keys should always be
     * the result of this method.  It's not up to callers to worry about; they can pass
     * in show names however they have them, and the methods here will be sure to
     * normalize them in preparation for querying.
     *
     * @param showName
     *            the substring of the file path that we think represents the show name
     * @return a version of the show name that is more suitable for a query; this may
     *         include case normalization, removal of superfluous whitepsace and
     *         punctuation, etc.
     */
    public static String makeQueryString(String showName) {
        return StringUtils.replacePunctuation(showName).toLowerCase();
    }

    public static void showStore() {
        for (String n : _shows.keySet()) {
            List<Show> shows = _shows.get(n);
            for (Show show : shows) {
                logger.fine(n + " => " + show.getName() + " [" + show.getId() + "]");
            }
        }
    }


    private static void notifyListeners(String showKey, List<Show> shows) {
        logger.fine("inside notifyListeners");
        ShowRegistrations registrations = _showRegistrations.get(showKey);

        if (registrations == null) {
            logger.fine("alas, no registrations for " + showKey);
        } else {
            for (ShowInformationListener informationListener : registrations.getListeners()) {
                logger.fine("notifying " + informationListener);
                informationListener.downloadComplete(shows);
            }
        }
    }

    /**
     * Add shows to the store, registered by the show name.<br />
     *
     * @param showName
     *            the show name
     * @param shows
     *            the {@link Show} options
     */
    static void addShows(String showName, List<Show> shows) {
        String showKey = makeQueryString(showName);
        logger.fine("Options and episodes for '" + showName + "' acquired");
        _shows.put(showKey, shows);
        notifyListeners(showKey, shows);
    }


    /**
     * Add a show to the store, registered by the show name.<br />
     * Added this distinct method to enable unit testing
     *
     * @param showName
     *            the show name
     * @param show
     *            the {@link Show}
     */
    public static void addShow(String showName, Show show) {
        List<Show> options = new ArrayList<>();
        options.add(show);
        addShows(showName, options);
    }

    private static void downloadShow(final String queryString) {
        if ((queryString == null) || (queryString.length() < 2)) {
            logger.fine("not trying to look up too-short name (\"" + queryString + "\")");
            return;
        }
        Callable<Boolean> showFetcher = new Callable<Boolean>() {
                @Override
                public Boolean call() throws InterruptedException {
                    List<Show> options;
                    try {
                        logger.fine("looking for options for " + queryString);
                        options = TheTVDBProvider.getShowOptions(queryString);
                        logger.fine("got options for " + queryString);
                    } catch (GenericException e) {
                        logger.fine("exception getting options for " + queryString);
                        addShow(queryString, new FailedShow("", queryString, "", e));
                        return true;
                    }
                    int nOptions = (options == null) ? 0 : options.size();
                    if (nOptions == 0) {
                        logger.fine("did not find any options for " + queryString);
                        addShow(queryString, new FailedShow(queryString));
                        return true;
                    }
                    try {
                        addShows(queryString, options);
                        for (Show s : options) {
                            TheTVDBProvider.getShowListing(s);
                        }
                    } catch (GenericException e) {
                        logger.fine("exception getting episodes for " + queryString);
                        addShow(queryString, new FailedShow("", queryString, "", e));
                    }

                    return true;
                }
            };
        threadPool.submit(showFetcher);
    }

    private static List<Show> mapStringToShows(String queryString) {
        List<Show> shows = _shows.get(queryString);
        if ((shows == null) || (shows.size() == 0)) {
            // TODO: is this really possible?  In the case where the show is not found,
            // we still map the name to a "FailedShow".  So how can it be null?
            // Answer: one way is if this gets called before the downloading is finished.
            // In that case, maybe we should call the method with the listener...
            GenericException e = new GenericException("No show found for show name: '"
                                                               + queryString + "'");
            FailedShow notFound = new FailedShow("", queryString, "", e);
            List<Show> options = new ArrayList<>();
            options.add(notFound);
            addShows(queryString, options);
            return options;
        }

        return shows;
    }

    /**
     * <p>
     * Download the show details if required, otherwise notify listener.
     * </p>
     * <ul>
     * <li>if we have already downloaded the show (exists in _shows) then just call the method on the listener</li>
     * <li>if we don't have the show, but are in the process of downloading the show (exists in _showRegistrations) then
     * add the listener to the registration</li>
     * <li>if we don't have the show and aren't downloading, then create the registration, add the listener and kick off
     * the download</li>
     * </ul>
     *
     * @param showName
     *            the name of the show
     * @param listener
     *            the listener to notify or register
     */
    public static void mapStringToShows(String showName, ShowInformationListener listener) {
        logger.fine("beginning process on " + showName);
        if (showName == null) {
            listener.downloadComplete(null);
        }
        String queryString = makeQueryString(showName);
        if (queryString.length() < 2) {
            List<Show> nope = new ArrayList<>();
            nope.add(new FailedShow("too short!"));
            listener.downloadComplete(nope);
        }
        List<Show> shows = _shows.get(queryString);
        if (shows != null) {
            logger.fine("already know about " + showName);
            listener.downloadComplete(shows);
        } else {
            ShowRegistrations registrations = _showRegistrations.get(queryString);
            if (registrations != null) {
                registrations.addListener(listener);
            } else {
                registrations = new ShowRegistrations();
                registrations.addListener(listener);
                _showRegistrations.put(queryString, registrations);
                logger.fine("trying to download " + showName);
                downloadShow(queryString);
            }
        }
    }

    public static void cleanUp() {
        threadPool.shutdownNow();
    }

    public static void clear() {
        _shows.clear();
        _showRegistrations.clear();
    }
}
