package org.tvrenamer.controller;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.tvrenamer.model.AppData;
import org.tvrenamer.model.EpisodeInfo;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.SeriesInfo;
import org.tvrenamer.model.ShowName;
import org.tvrenamer.model.TVRenamerIOException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TheTVDBSwaggerProvider {
    private static final Logger logger = Logger.getLogger(TheTVDBSwaggerProvider.class.getName());

    private static final String API_KEY = "4A9560FF0B2670B2";

    private static final String BASE_URL = "https://api.thetvdb.com";
    private static final String SEARCH_URL = BASE_URL + "/search/series?name=";
    private static final String LISTINGS_URL = BASE_URL + "/series/";
    private static final String EPISODES = "/episodes";
    private static final String LOGIN_URL = "/login";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient CLIENT = new OkHttpClient();

    private static class LoginResponse {
        String token;
    }

    private static class SeriesSearchResponse {
        List<SeriesInfo> data;
    }

    static class TvdbEpisode {
        String airedEpisodeNumber;
        String airedSeason;
        String dvdEpisodeNumber;
        String dvdSeason;
        String episodeName;
        String firstAired;
        String id;
    }

    static class Links {
        Integer next;
    }

    static class EpisodesResponse {
        Links links;
        List<TvdbEpisode> data;
    }

    private static String login() throws IOException {
        String json = "{\"apikey\":\"" + API_KEY + "\"}";
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(BASE_URL + LOGIN_URL)
                .post(body)
                .build();
        Response response = CLIENT.newCall(request).execute();
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            logger.warning("got null response body trying to get login");
            return null;
        }

        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<LoginResponse> adapter = moshi.adapter(LoginResponse.class);

        LoginResponse lResponse = adapter.fromJson(responseBody.string());
        if (lResponse == null) {
            logger.warning("got null login response");
            return null;
        }
        String token = lResponse.token;
        AppData.getInstance().setApiToken(token);
        return token;
    }

    private static String get(final String url) throws IOException {
        String jwt = AppData.getInstance().getApiToken();
        if (jwt == null) {
            jwt = login();
        }

        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + jwt)
                .url(url)
                .build();

        Response response = CLIENT.newCall(request).execute();
        switch (response.code()) {
            case 401:
            case 403:
                login();
                return get(url);
            default:
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    logger.info("got null response body with status "
                                + response.code() + " on get " + url);
                    return "";
                }
                return responseBody.string();
        }
    }

    /**
     * Fetch the show options from the provider, for the given show name.
     *
     * @param showName
     *   the show name to fetch the options for
     * @throws TVRenamerIOException if anything else goes wrong; this could
     *   include network difficulties or difficulty parsing the XML.
     */
    public static void getSeriesOptions(final ShowName showName) throws TVRenamerIOException {
        String searchUrl = SEARCH_URL + showName.getQueryString();
        String foundName = showName.getExampleFilename();
        String responseJSON = "";
        try {
            responseJSON = get(searchUrl);
            if (responseJSON != null) {
                Moshi moshi = new Moshi.Builder().build();
                JsonAdapter<SeriesSearchResponse> adapter = moshi.adapter(SeriesSearchResponse.class);
                SeriesSearchResponse searchResponse = adapter.fromJson(responseJSON);
                if (searchResponse != null) {
                    List<SeriesInfo> seriesList = searchResponse.data;
                    if (seriesList != null) {
                        seriesList.forEach(showName::addShowOption);
                    } else {
                        logger.fine("got no series options for " + foundName);
                    }
                } else {
                    logger.warning("got null search response for " + foundName);
                }
            } else {
                logger.warning("got null response while trying to get options for " + foundName);
            }
        } catch (IOException e) {
            String msg = "error parsing JSON from " + responseJSON + " for series " + foundName;
            logger.log(Level.WARNING, msg, e);
            throw new TVRenamerIOException(msg, e);
        }
    }

    private static void addEpisodesToSeries(final Series series, final List<TvdbEpisode> episodeList) {
        int nEpisodes = episodeList.size();

        EpisodeInfo[] episodeInfos = new EpisodeInfo[nEpisodes];
        int i = 0;

        for (TvdbEpisode episode : episodeList) {
            episodeInfos[i] = new EpisodeInfo.Builder()
                .episodeId(episode.id)
                .seasonNumber(episode.airedSeason)
                .episodeNumber(episode.airedEpisodeNumber)
                .episodeName(episode.episodeName)
                .firstAired(episode.firstAired)
                .dvdSeason(episode.dvdSeason)
                .dvdEpisodeNumber(episode.dvdEpisodeNumber)
                .build();
            i++;
        }

        series.addEpisodeInfos(episodeInfos);
    }

    private static String getSeriesEpisodes(final Integer id, final Integer page) throws IOException {
        return get(LISTINGS_URL + id + EPISODES + "?page=" + page);
    }

    private static String getSeriesEpisodes(final Integer id) throws IOException {
        return get(LISTINGS_URL + id + EPISODES);
    }

    private static void readEpisodesFromSearchResponse(final String response, final Series series)
        throws IOException
    {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<EpisodesResponse> adapter = moshi.adapter(EpisodesResponse.class);
        if (adapter == null) {
            logger.warning("failed to create moshi adapter while trying to read episodes");
            return;
        }

        EpisodesResponse episodesResponse = adapter.fromJson(response);
        if (episodesResponse == null) {
            logger.warning("got null parsing JSON episodes response");
            return;
        }

        if (episodesResponse.links.next != null) {
            String episodes = getSeriesEpisodes(series.getId(), episodesResponse.links.next);
            readEpisodesFromSearchResponse(episodes, series);
        }

        List<TvdbEpisode> episodeList = episodesResponse.data;
        addEpisodesToSeries(series, episodeList);
    }

    /**
     * Fetch the episode listings from the provider, for the given Series.
     *
     * @param series
     *   the Series to fetch the episode listings for
     * @throws TVRenamerIOException if anything goes wrong; this could include
     *   network difficulties, difficulty parsing the XML, or problems parsing
     *   data expected to be numeric.
     */
    public static void getSeriesListing(final Series series) throws TVRenamerIOException {
        String responseJSON = "";
        try {
            responseJSON = getSeriesEpisodes(series.getId());
            readEpisodesFromSearchResponse(responseJSON, series);
            series.listingsSucceeded();
        } catch (IOException e) {
            String msg = "error parsing JSON from " + responseJSON + " for series #" + series.getId();
            logger.log(Level.WARNING, msg, e);
            throw new TVRenamerIOException(msg, e);
        }
    }
}
