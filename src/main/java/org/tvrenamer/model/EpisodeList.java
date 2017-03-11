package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.EpisodeListPersistence;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class EpisodeList {
    private static Logger logger = Logger.getLogger(EpisodeList.class.getName());

    private final String showName;
    private final String showId;
    private List<Episode> episodes;

    public EpisodeList(int n, String showName, String showId) {
        this.showName = showName;
        this.showId = showId;
        episodes = new ArrayList<>(n);
    }

    public boolean addEpisode(Episode ep) {
        episodes.add(ep);
        return true;
    }

    private static EpisodeList load(Path episodeFile) {
        EpisodeList episodeList = EpisodeListPersistence.retrieve(episodeFile);

        if (episodeList == null) {
            logger.warning("could not read episode list from "
                           + episodeFile.toAbsolutePath());
        } else {
            logger.fine("Sucessfully read episodeList from: " + episodeFile.toAbsolutePath());
            logger.fine("Sucessfully read episodeList: " + episodeList.toString());
        }

        return episodeList;
    }

    public void store(Path episodeFile) {
        EpisodeListPersistence.persist(this, episodeFile);
        logger.fine("Sucessfully saved/updated episodeList");
    }
}
