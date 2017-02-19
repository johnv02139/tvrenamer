package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.IMDB_BASE_URL;

import org.tvrenamer.controller.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a TV Series, with a name, url and list of seasons.
 */
public class Series implements Comparable<Series> {
    private final int id;
    private final String name;
    private final String nameKey;
    private final String dirName;
    private final String idString;
    private final String imdb;

    private final Map<Integer, Season> seasons;

    public Series(String name, String idString, String imdb) {
        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("series ID must be an integer, not " + idString);
        }
        this.name = name;
        nameKey = name.toLowerCase();
        dirName = StringUtils.sanitiseTitle(name);
        this.idString = idString;
        this.imdb = imdb;

        seasons = new ConcurrentHashMap<>();
    }

    public Series(String name, String id) {
        this(name, id, null);
    }

    public int getId() {
        return id;
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

    public String getDirName() {
        return dirName;
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

    public int getSeasonCount() {
        return seasons.size();
    }

    public boolean hasSeasons() {
        return (seasons.size() > 0);
    }

    @Override
    public String toString() {
        return "Series [" + name + ", id=" + id + ", imdb=" + imdb + ", " + seasons.size() + " seasons]";
    }

    public String toLongString() {
        return "Series [id=" + id + ", name=" + name + ", imdb=" + imdb + ", seasons=" + seasons + "]";
    }

    @Override
    public int compareTo(Series other) {
        return other.id - id;
    }
}
