package org.tvrenamer.model;


import static org.tvrenamer.model.util.Constants.IMDB_BASE_URL;

import org.tvrenamer.controller.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a TV Series, with a name, url and list of seasons.
 */
public class Series implements Comparable<Series> {
    private final int idNum;
    private final String name;
    private final String nameKey;
    private final String idString;
    private final String imdb;

    private final Map<Integer, Season> seasons;

    public Series(String name, String idString, String imdb) {
        try {
            idNum = Integer.parseInt(idString);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("series ID must be an integer, not " + idString);
        }
        this.name = name;
        nameKey = name.toLowerCase();
        this.idString = idString;
        this.imdb = imdb;

        seasons = new ConcurrentHashMap<>();
    }

    public Series(String name, String id) {
        this(name, id, null);
    }

    public int getId() {
        return idNum;
    }

    private boolean addEpisode(Episode ep) {
        String seasonNumText = ep.getSeasonNumberText();
        Integer seasonNum = StringUtils.stringToInt(seasonNumText);
        if (seasonNum == null) {
            return false;
        }
        Season season;
        if (seasons.containsKey(seasonNum)) {
            season = seasons.get(seasonNum);
        } else {
            season = new Season(this, seasonNumText);
            setSeason(seasonNum, season);
        }
        season.addEpisode(ep);
        return true;
    }

    public void addEpisodes(Episode[] episodes) {
        for (int i = 0; i < episodes.length; i++) {
            if (episodes[i] != null) {
                addEpisode(episodes[i]);
            }
        }
    }

    public String getIdString() {
        return idString;
    }

    public String getName() {
        return name;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getUrl() {
        return (imdb == null) ? "" : IMDB_BASE_URL + imdb;
    }

    public void setSeason(int sNum, Season season) {
        seasons.put(sNum, season);
    }

    public void setSeason(String sNumText, Season season) {
        Integer sNum = StringUtils.stringToInt(sNumText);
        seasons.put(sNum, season);
    }

    public Season getSeason(int sNum) {
        if (seasons.containsKey(sNum)) {
            return seasons.get(sNum);
        } else {
            return null;
        }
    }

    public Season getSeason(String sNumText) {
        Integer sNum = StringUtils.stringToInt(sNumText);
        if (sNum == null) {
            return null;
        }
        return getSeason(sNum);
    }

    public int getSeasonCount() {
        return seasons.size();
    }

    public boolean hasSeasons() {
        return (seasons.size() > 0);
    }

    @Override
    public String toString() {
        return "Series [" + name + ", id=" + idString + ", imdb=" + imdb + ", " + seasons.size() + " seasons]";
    }

    public String toLongString() {
        return "Series [id=" + idString + ", name=" + name + ", imdb=" + imdb + ", seasons=" + seasons + "]";
    }

    @Override
    public int compareTo(Series other) {
        return other.idNum - idNum;
    }
}
