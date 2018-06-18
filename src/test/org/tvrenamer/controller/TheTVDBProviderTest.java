package org.tvrenamer.controller;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.tvrenamer.model.Episode;
import org.tvrenamer.model.Series;

public class TheTVDBProviderTest {

    @Test
    public void testQuerySeriesName() throws Exception {
        for (Series show : TheTVDBProvider.querySeriesName("Gossip Girl")) {
            assertNotNull(show);
            assertNotEquals(0, show.getName().length());
            assertNotEquals(0, show.getIdString().length());
        }
    }

    @Test
    public void testGetListings() throws Exception {
        String seriesId = "80547";
        String seriesName = "Gossip Girl";
        Series gossip = new Series(seriesName, seriesId);
        Episode[] episodes = TheTVDBProvider.getListings(seriesId, seriesName);
        assertNotEquals(0, episodes.length);
    }
}
