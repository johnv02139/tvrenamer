package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.model.Episode;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.Season;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.ShowStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * There are three major steps to turning a filename into real show information.
 *
 * First is we parse the filename, and attempt to identify the parts of the
 * filename that represents the show name, the season number, the episode
 * number, and possibly the screen resolution.  For the substring that we think
 * identifies the show name, we normalize it somewhat.  We replace punctuation
 * and lower-case the name.
 *
 * The next step is to take that normalized string and send it to the
 * provider to try to figure out which show this is actually referring to.
 * The provider might return any number of results, including zero.  If
 * it returns more than one, we try to select the right one.
 *
 * Once we have identified the actual show, then we use the season and
 * episode information to look up the actual episode.
 *
 * This first step in the process is tested by FilenameParserTest.  This file
 * tests the latter two steps.  The data in the variable "values" has all the
 * information we need to verify the last two steps.
 *
 * For each filename, we provide the proper, formatted name of the actual
 * show; this is the string we expect to find after the show name has been
 * resolved based on information from the provider.
 *
 * We currently also include a "query string", which is not relevant to these
 * tests, and should be removed.
 *
 */
public class TheTVDBProviderTest {


    private static class TestInput {
        public final String input;
        public final String actualShowName;
        public final String season;
        public final String episode;

        public final String episodeTitle;
        public final String episodeResolution;

        public TestInput(String input, String actualShowName, String season, String episode,
                         String episodeTitle, String episodeResolution)
        {
            this(input, null, actualShowName, season, episode, episodeTitle, episodeResolution);
        }

        public TestInput(String input, String maybeQueryString, String actualShowName,
                         String season, String episode, String episodeTitle, String episodeResolution)
        {
            this.input = input;
            this.actualShowName = actualShowName;
            this.season = season;
            this.episode = episode;

            this.episodeTitle = episodeTitle;
            this.episodeResolution = episodeResolution;
        }
    }

    private static final List<TestInput> values0 = new ArrayList<>(10);
    private static final List<TestInput> values1 = new ArrayList<>(50);
    private static final List<TestInput> values2 = new ArrayList<>(7);

    private static boolean testAllInputs = true;

    private static void setupValues0() {
        // In this section, we have, in order:
        // (1) actual filename to test
        // (2) the query string; that is, what we expect to pull out of the filename, to use as a basis to
        //     query for the show
        // (3) the official name of the show we expect to match, as we expect the provider to return it
        // (4) the season number, as a (non-padded) string
        // (5) the episode number, as a (non-padded) string
        // (6) the title of the episode, exactly as we expect the provider to return it
        // (7) the screen resolution found in the filename
        values0.add(new TestInput("American.Dad.S09E17.HDTV.x264-2HD.mp4",
                                 "american dad", "American Dad!", "9", "17",
                                 "The Full Cognitive Redaction of Avery Bullock by the Coward Stan Smith", ""));
        if (testAllInputs) {
            values0.add(new TestInput("Nip.Tuck.S06E01.720p.HDTV.X264-DIMENSION.mkv", "Nip/Tuck", "6", "1",
                                        "Don Hoberman", "720p"));
            values0.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET",
                                        "Marvel's Agents of S.H.I.E.L.D.", "3", "3", "A Wanted (Inhu)man", ""));
            values0.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E10.HDTV.x264-KILLERS",
                                        "Marvel's Agents of S.H.I.E.L.D.", "3", "10", "Maveth", ""));
            values0.add(new TestInput("human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv", "Human Target (2010)",
                                        "1", "2", "Rewind", "720p"));
            values0.add(new TestInput("castle.2009.s01e09.720p.hdtv.x264-ctu.mkv", "Castle (2009)", "1", "9",
                                        "Little Girl Lost", "720p"));
            values0.add(new TestInput("Reign.2013.S01E20.HDTV.x264-2HD.mp4", "Reign (2013)", "1", "20",
                                        "Higher Ground", ""));
            values0.add(new TestInput("The.Americans.2013.S02E10.HDTV.x264-LOL.mp4", "The Americans (2013)",
                                        "2", "10", "Yousaf", ""));
            values0.add(new TestInput("law.and.order.svu.1705.hdtv-lol", "Law & Order: Special Victims Unit",
                                        "17", "05", "Community Policing", ""));
            values0.add(new TestInput("House.Of.Cards.2013.S01E06.HDTV.x264-EVOLVE.mp4", "House of Cards (US)",
                                        "1", "6", "Chapter 6", ""));
        }
    }

    private static void setupValues1() {
        // In this section, we do not supply an explicit query string.  The query string is simply
        //  the show name, lower-cased. So, we have:
        // (1) actual filename to test
        // (2) the official name of the show we expect to match, as we expect the provider to return it,
        //     which, after being lower-cased, is also what we expect to pull out of the filename
        // (3) the season number, as a (non-padded) string
        // (4) the episode number, as a (non-padded) string
        // (5) the title of the episode, exactly as we expect the provider to return it
        // (6) the screen resolution found in the filename
        values1.add(new TestInput("Modern.Family.S05E12.HDTV.x264-EXCELLENCE.mp4", "Modern Family", "5", "12",
                                    "Under Pressure", ""));
        if (testAllInputs) {
            values1.add(new TestInput("game.of.thrones.5x01.mp4", "Game of Thrones", "5", "1", "The Wars to Come",
                                        ""));
            values1.add(new TestInput("24.s08.e01.720p.hdtv.x264-immerse.mkv", "24", "8", "1",
                                        "Day 8: 4:00 P.M. - 5:00 P.M.", "720p"));
            values1.add(new TestInput("24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv", "24", "7", "18",
                                        "Day 7: 1:00 A.M. - 2:00 A.M.", "720p"));
            values1.add(new TestInput("dexter.407.720p.hdtv.x264-sys.mkv", "Dexter", "4", "7", "Slack Tide",
                                        "720p"));
            values1.add(new TestInput("JAG.S10E01.DVDRip.XviD-P0W4DVD.avi", "JAG", "10", "1",
                                        "Hail and Farewell, Part II (2)", ""));
            values1.add(new TestInput("Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv", "Lost", "6", "5",
                                        "Lighthouse", "720p"));
            values1.add(new TestInput("warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv", "Warehouse 13", "1", "1",
                                        "Pilot", "720p"));
            values1.add(new TestInput("one.tree.hill.s07e14.hdtv.xvid-fqm.avi", "One Tree Hill", "7", "14",
                                        "Family Affair", ""));
            values1.add(new TestInput("gossip.girl.s03e15.hdtv.xvid-fqm.avi", "Gossip Girl", "3", "15",
                                        "The Sixteen Year Old Virgin", ""));
            values1.add(new TestInput("smallville.s09e14.hdtv.xvid-xii.avi", "Smallville", "9", "14", "Conspiracy",
                                        ""));
            values1.add(new TestInput("smallville.s09e15.hdtv.xvid-2hd.avi", "Smallville", "9", "15", "Escape",
                                        ""));
            values1.add(new TestInput("the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv", "The Big Bang Theory",
                                        "3", "18", "The Pants Alternative", "720p"));
            values1.add(new TestInput("/TV/Dexter/S05E05 First Blood.mkv", "Dexter", "5", "5", "First Blood", ""));
            values1.add(new TestInput("/TV/Lost/Lost [2x07].mkv", "Lost", "2", "7", "The Other 48 Days", ""));
            values1.add(new TestInput("Californication.S07E04.HDTV.x264-2HD.mp4", "Californication", "7", "4",
                                        "Dicks", ""));
            values1.add(new TestInput("Continuum.S03E07.HDTV.x264-2HD.mp4", "Continuum", "3", "7", "Waning Minutes",
                                        ""));
            values1.add(new TestInput("Elementary.S02E23.HDTV.x264-LOL.mp4", "Elementary", "2", "23",
                                        "Art in the Blood", ""));
            values1.add(new TestInput("Family.Guy.S12E19.HDTV.x264-2HD.mp4", "Family Guy", "12", "19",
                                        "Meg Stinks!", ""));
            values1.add(new TestInput("Fargo.S01E01.HDTV.x264-2HD.mp4", "Fargo", "1", "1",
                                        "The Crocodile's Dilemma", ""));
            values1.add(new TestInput("Girls.S03E11.HDTV.x264-KILLERS.mp4", "Girls", "3", "11", "I Saw You", ""));
            values1.add(new TestInput("Grimm.S03E19.HDTV.x264-LOL.mp4", "Grimm", "3", "19",
                                        "Nobody Knows the Trubel I've Seen", ""));
            values1.add(new TestInput("New.Girl.S03E23.HDTV.x264-LOL.mp4", "New Girl", "3", "23", "Cruise", ""));
            values1.add(new TestInput("Nurse.Jackie.S06E04.HDTV.x264-2HD.mp4", "Nurse Jackie", "6", "4",
                                        "Jungle Love", ""));
            values1.add(new TestInput("Offspring - S05E01.mp4", "Offspring", "5", "1", "Back in the Game", ""));
            values1.add(new TestInput("Robot.Chicken.S07E04.PROPER.HDTV.x264-W4F.mp4", "Robot Chicken", "7", "4",
                                        "Rebel Appliance", ""));
            values1.add(new TestInput("Supernatural.S09E21.HDTV.x264-LOL.mp4", "Supernatural", "9", "21",
                                        "King of the Damned", ""));
            values1.add(new TestInput("The.Big.Bang.Theory.S07E23.HDTV.x264-LOL.mp4", "The Big Bang Theory",
                                        "7", "23", "The Gorilla Dissolution", ""));
            values1.add(new TestInput("The.Good.Wife.S05E20.HDTV.x264-LOL.mp4", "The Good Wife", "5", "20",
                                        "The Deep Web", ""));
            values1.add(new TestInput("The.Walking.Dead.S04E16.PROPER.HDTV.x264-2HD.mp4", "The Walking Dead",
                                        "4", "16", "A", ""));
            values1.add(new TestInput("Veep.S03E05.HDTV.x264-KILLERS.mp4", "Veep", "3", "5", "Fishing", ""));
            values1.add(new TestInput("Witches.of.East.End.S01E01.PROPER.HDTV.x264-2HD.mp4", "Witches of East End",
                                        "1", "1", "Pilot", ""));
            values1.add(new TestInput("Warehouse.13.S05E04.HDTV.x264-2HD.mp4", "Warehouse 13", "5", "4",
                                        "Savage Seduction", ""));
            values1.add(new TestInput("the.100.208.hdtv-lol.mp4", "The 100", "2", "8", "Spacewalker", ""));
            values1.add(new TestInput("firefly.1x01.hdtv-lol.mp4", "Firefly", "1", "1", "The Train Job", ""));
            values1.add(new TestInput("firefly.1x02.hdtv-lol.mp4", "Firefly", "1", "2", "Bushwhacked", ""));
            values1.add(new TestInput("firefly.1x03.hdtv-lol.mp4", "Firefly", "1", "3", "Our Mrs. Reynolds", ""));
            values1.add(new TestInput("firefly.1x04.hdtv-lol.mp4", "Firefly", "1", "4", "Jaynestown", ""));
            values1.add(new TestInput("firefly.1x05.hdtv-lol.mp4", "Firefly", "1", "5", "Out of Gas", ""));
            values1.add(new TestInput("firefly.1x06.hdtv-lol.mp4", "Firefly", "1", "6", "Shindig", ""));
            values1.add(new TestInput("firefly.1x07.hdtv-lol.mp4", "Firefly", "1", "7", "Safe", ""));
            values1.add(new TestInput("firefly.1x08.hdtv-lol.mp4", "Firefly", "1", "8", "Ariel", ""));
            values1.add(new TestInput("firefly.1x09.hdtv-lol.mp4", "Firefly", "1", "9", "War Stories", ""));
            values1.add(new TestInput("firefly.1x10.hdtv-lol.mp4", "Firefly", "1", "10", "Objects in Space", ""));
            values1.add(new TestInput("firefly.1x11.hdtv-lol.mp4", "Firefly", "1", "11", "Serenity", ""));
            values1.add(new TestInput("firefly.1x12.hdtv-lol.mp4", "Firefly", "1", "12", "Heart of Gold", ""));
            values1.add(new TestInput("firefly.1x13.hdtv-lol.mp4", "Firefly", "1", "13", "Trash", ""));
            values1.add(new TestInput("firefly.1x14.hdtv-lol.mp4", "Firefly", "1", "14", "The Message", ""));
            values1.add(new TestInput("Strike.Back.S01E01.Mini.720p.HDTV.DD5.1.x264.mkv", "Strike Back", "1", "1",
                                        "Chris Ryan's Strike Back, Episode 1", "720p"));
            values1.add(new TestInput("ncis.1304.hdtv-lol", "NCIS", "13", "04", "Double Trouble", ""));
        }
    }

    private static void setupValues2() {
        // More tests added to test screen resolution
        values2.add(new TestInput("The.Big.Bang.Theory.S10E04.720p.HDTV.X264-DIMENSION[ettv].mkv",
                                    "The Big Bang Theory", "10", "4",
                                    "The Cohabitation Experimentation", "720p"));
        if (testAllInputs) {
            values2.add(new TestInput("Lucifer.S02E03.720p.HDTV.X264-DIMENSION[ettv].mkv", "Lucifer", "2", "3",
                                        "Sin-Eater", "720p"));
            values2.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].mkv",
                                        "Marvel's Agents of S.H.I.E.L.D.", "4", "3", "Uprising", "1080p"));
            values2.add(new TestInput("Supernatural.S11E22.1080p.HDTV.X264-DIMENSION[ettv].mkv", "Supernatural",
                                        "11", "22", "We Happy Few", "1080p"));
            values2.add(new TestInput("Supernatural.S11E22.HDTV.X264-DIMENSION.720p.[ettv].mkv", "Supernatural",
                                        "11", "22", "We Happy Few", "720p"));
            values2.add(new TestInput("Channel.Zero.S01E01.480p.HDTV.X264-DIMENSION[ettv].mkv", "Channel Zero",
                                        "1", "1", "You Have to Go Inside", "480p"));
            values2.add(new TestInput("NCIS.S14E04.720p.HDTV.X264-DIMENSION[ettv].mkv", "NCIS", "14", "4",
                                        "Love Boat", "720p"));
        }
    }

    @BeforeClass
    public static void setupValues() {
        setupValues0();
        setupValues1();
        setupValues2();
    }

    // Once we have a CompletableFuture, we need to complete it.  There are a few ways, but
    // obviously the simplest is to call complete().  If we simply call the JUnit method
    // fail(), the future thread does not die and the test never exits.  The same appears
    // to happen with an uncaught exception.  So, be very careful to make sure, one way or
    // other, we call complete.

    // Of course, none of this matters when everything works.  But if we want failure cases
    // to actually stop and report failure, we need to complete the future, one way or another.

    // We use a brief failure message as the show title in cases where we detect failure.
    // Just make sure to not add a test case where the actual episode's title is one of
    // the failure messages.  :)
    private Series testQuerySeries(final TestInput testInput, final FileEpisode fileEpisode) {
        try {
            String filenameSeries = fileEpisode.getFilenameSeries();

            final CompletableFuture<Series> futureShow = new CompletableFuture<>();
            ShowStore.mapStringToShow(filenameSeries, new SeriesLookupListener() {
                    @Override
                    public void downloadComplete(Series show) {
                        futureShow.complete(show);
                    }

                    @Override
                    public void downloadFailed(Series show) {
                        futureShow.complete(null);
                    }
                });
            Series gotSeries = futureShow.get();
            if (gotSeries == null) {
                fail("could not parse series name input " + filenameSeries);
                return null;
            }
            assertEquals(testInput.actualShowName, gotSeries.getName());
            return gotSeries;
        } catch (Exception e) {
            fail(e.getMessage());
            return null;
        }
    }

    private boolean testDownloadEpisode(TestInput testInput) {
        try {
            final FileEpisode fileEpisode = new FileEpisode(testInput.input);
            assertTrue(fileEpisode.wasParsed());

            final Series series = testQuerySeries(testInput, fileEpisode);

            final String seasonNum = fileEpisode.getFilenameSeason();
            final String episode = fileEpisode.getFilenameEpisode();
            final CompletableFuture<String> future = new CompletableFuture<>();
            ListingsLookup.getListings(series, new EpisodeListListener() {
                    @Override
                    public void downloadListingsComplete(Series show) {
                        Season season = show.getSeason(seasonNum);
                        if (season == null) {
                            future.complete("no season");
                        }
                        String title = season.getTitle(episode);
                        if (title == null) {
                            future.complete("no title");
                        } else {
                            future.complete(title);
                        }
                    }

                    @Override
                    public void downloadListingsFailed(Series show) {
                        future.complete("downloadFailed");
                    }
                });
            assertEquals(testInput.episodeTitle, future.get());
            return true;
        } catch (Exception e) {
            fail(e.getMessage());
            return false;
        }
    }

    public void testDownloadEpisodes(List<TestInput> valList) {
        try {
            for (TestInput testInput : valList) {
                if (testInput.episodeTitle == null) {
                    fail("bad test input: no title");
                } else {
                    assertTrue(testDownloadEpisode(testInput));
                }
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testDownloadEpisodes0() {
        testDownloadEpisodes(values0);
    }

    @Test
    public void testDownloadEpisodes1() {
        testDownloadEpisodes(values1);
    }

    @Test
    public void testDownloadEpisodes2() {
        testDownloadEpisodes(values2);
    }

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
        String seriesName = "Gossip Girl";
        String seriesId = "80547";
        Series gossip = new Series(seriesName, seriesId);
        Episode[] episodes = TheTVDBProvider.getListings(seriesId, seriesName);
        assertNotEquals(0, episodes.length);
    }
}
