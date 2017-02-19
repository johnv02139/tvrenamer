package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.model.FileEpisode;

import java.util.LinkedList;
import java.util.List;

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
 * This file attempts to test all three steps.  The static data provided
 * in "values" has all the information we need to verify all three steps.
 *
 * For each filename, we provide the proper, formatted name of the actual
 * show; this is the string we expect to find after the show name has been
 * resolved based on information from the provider.
 *
 * That's the second step.  To test the first step, we might provide the
 * string that we expect to get after we've parsed the filename and normalized
 * the substring.  In this file, we refer to that as the "query string",
 * because it's the string we send to the provider for the query.  But
 * the static data does not necessarily give the query string explicitly.
 * If the query string is just the lower-cased version of the actual show
 * name, we don't make it explicit.
 *
 * That's why some of the values use the six-arg constructor, and some use
 * the seven-arg.  Basically if there's punctuation in the show name, we
 * need to provide the query string explicitly, but if the query string is
 * identical to the show name, apart from case, we can infer it.
 *
 */
public class TVRenamerTest {

    private static class TestInput {
        public final String input;
        public final String queryString;
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
            String queryString = (maybeQueryString == null) ? actualShowName.toLowerCase() : maybeQueryString;

            this.input = input;
            this.queryString = queryString;
            this.actualShowName = actualShowName;
            this.season = season;
            this.episode = episode;

            this.episodeTitle = episodeTitle;
            this.episodeResolution = episodeResolution;
        }
    }

    public static final List<TestInput> values = new LinkedList<>();

    @BeforeClass
    public static void setupValues() {
        // In this section, we have, in order:
        // (1) actual filename to test
        // (2) the query string; that is, what we expect to pull out of the filename, to use as a basis to
        //     query for the show
        // (3) the official name of the show we expect to match, as we expect the provider to return it
        // (4) the season number, as a (non-padded) string
        // (5) the episode number, as a (non-padded) string
        // (6) the title of the episode, exactly as we expect the provider to return it
        // (7) the screen resolution found in the filename
        values.add(new TestInput("Nip.Tuck.S06E01.720p.HDTV.X264-DIMENSION.mkv",
                                 "nip tuck", "Nip/Tuck", "6", "1", "Don Hoberman", "720p"));
        values.add(new TestInput("American.Dad.S09E17.HDTV.x264-2HD.mp4",
                                 "american dad", "American Dad!", "9", "17",
                                 "The Full Cognitive Redaction of Avery Bullock by the Coward Stan Smith", ""));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET",
                                 "marvels agents of shield", "Marvel's Agents of S.H.I.E.L.D.",
                                 "3", "3", "A Wanted (Inhu)man", ""));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E10.HDTV.x264-KILLERS",
                                 "marvels agents of shield", "Marvel's Agents of S.H.I.E.L.D.",
                                 "3", "10", "Maveth", ""));

        values.add(new TestInput("human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv",
                                 "human target 2010", "Human Target (2010)",
                                 "1", "2", "Rewind", "720p"));
        values.add(new TestInput("castle.2009.s01e09.720p.hdtv.x264-ctu.mkv",
                                 "castle 2009", "Castle (2009)",
                                 "1", "9", "Little Girl Lost", "720p"));
        values.add(new TestInput("Reign.2013.S01E20.HDTV.x264-2HD.mp4",
                                 "reign 2013", "Reign (2013)",
                                 "1", "20", "Higher Ground", ""));
        values.add(new TestInput("The.Americans.2013.S02E10.HDTV.x264-LOL.mp4",
                                 "the americans 2013", "The Americans (2013)",
                                 "2", "10", "Yousaf", ""));

        values.add(new TestInput("law.and.order.svu.1705.hdtv-lol",
                                 "law and order svu", "Law & Order: Special Victims Unit",
                                 "17", "05", "Community Policing", ""));
        values.add(new TestInput("House.Of.Cards.2013.S01E06.HDTV.x264-EVOLVE.mp4",
                                 "house of cards 2013", "House of Cards (US)", "1", "6", "Chapter 6", ""));


        // In this section, we do not supply an explicit query string.  The query string is simply
        //  the show name, lower-cased. So, we have:
        // (1) actual filename to test
        // (2) the official name of the show we expect to match, as we expect the provider to return it,
        //     which, after being lower-cased, is also what we expect to pull out of the filename
        // (3) the season number, as a (non-padded) string
        // (4) the episode number, as a (non-padded) string
        // (5) the title of the episode, exactly as we expect the provider to return it
        // (6) the screen resolution found in the filename
        values.add(new TestInput("game.of.thrones.5x01.mp4", "Game of Thrones", "5", "1", "The Wars to Come", ""));
        values.add(new TestInput("24.s08.e01.720p.hdtv.x264-immerse.mkv", "24", "8", "1",
                                 "Day 8: 4:00 P.M. - 5:00 P.M.", "720p"));
        values.add(new TestInput("24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv", "24", "7", "18",
                                 "Day 7: 1:00 A.M. - 2:00 A.M.", "720p"));
        values.add(new TestInput("dexter.407.720p.hdtv.x264-sys.mkv", "Dexter", "4", "7", "Slack Tide", "720p"));
        values.add(new TestInput("JAG.S10E01.DVDRip.XviD-P0W4DVD.avi", "JAG", "10", "1",
                                 "Hail and Farewell, Part II (2)", ""));
        values.add(new TestInput("Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv", "Lost", "6", "5",
                                 "Lighthouse", "720p"));
        values.add(new TestInput("warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv", "Warehouse 13", "1", "1", "Pilot", "720p"));
        values.add(new TestInput("one.tree.hill.s07e14.hdtv.xvid-fqm.avi", "One Tree Hill", "7", "14", "Family Affair", ""));
        values.add(new TestInput("gossip.girl.s03e15.hdtv.xvid-fqm.avi", "Gossip Girl", "3", "15",
                                 "The Sixteen Year Old Virgin", ""));
        values.add(new TestInput("smallville.s09e14.hdtv.xvid-xii.avi", "Smallville", "9", "14", "Conspiracy", ""));
        values.add(new TestInput("smallville.s09e15.hdtv.xvid-2hd.avi", "Smallville", "9", "15", "Escape", ""));
        values.add(new TestInput("the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv", "The Big Bang Theory", "3", "18",
                                 "The Pants Alternative", "720p"));
        values.add(new TestInput("/TV/Dexter/S05E05 First Blood.mkv", "Dexter", "5", "5", "First Blood", ""));
        values.add(new TestInput("/TV/Lost/Lost [2x07].mkv", "Lost", "2", "7", "The Other 48 Days", ""));

        values.add(new TestInput("Californication.S07E04.HDTV.x264-2HD.mp4", "Californication", "7", "4", "Dicks", ""));
        values.add(new TestInput("Continuum.S03E07.HDTV.x264-2HD.mp4", "Continuum", "3", "7", "Waning Minutes", ""));
        values.add(new TestInput("Elementary.S02E23.HDTV.x264-LOL.mp4", "Elementary", "2", "23", "Art in the Blood", ""));
        values.add(new TestInput("Family.Guy.S12E19.HDTV.x264-2HD.mp4", "Family Guy", "12", "19", "Meg Stinks!", ""));
        values.add(new TestInput("Fargo.S01E01.HDTV.x264-2HD.mp4", "Fargo", "1", "1", "The Crocodile's Dilemma", ""));
        values.add(new TestInput("Girls.S03E11.HDTV.x264-KILLERS.mp4", "Girls", "3", "11", "I Saw You", ""));
        values.add(new TestInput("Grimm.S03E19.HDTV.x264-LOL.mp4", "Grimm", "3", "19",
                                 "Nobody Knows the Trubel I've Seen", ""));
        values.add(new TestInput("Modern.Family.S05E12.HDTV.x264-EXCELLENCE.mp4", "Modern Family", "5", "12",
                                 "Under Pressure", ""));
        values.add(new TestInput("New.Girl.S03E23.HDTV.x264-LOL.mp4", "New Girl", "3", "23", "Cruise", ""));
        values.add(new TestInput("Nurse.Jackie.S06E04.HDTV.x264-2HD.mp4", "Nurse Jackie", "6", "4", "Jungle Love", ""));
        values.add(new TestInput("Offspring - S05E01.mp4", "Offspring", "5", "1", "Back in the Game", ""));
        values.add(new TestInput("Robot.Chicken.S07E04.PROPER.HDTV.x264-W4F.mp4", "Robot Chicken", "7", "4",
                                 "Rebel Appliance", ""));
        values.add(new TestInput("Supernatural.S09E21.HDTV.x264-LOL.mp4", "Supernatural", "9", "21",
                                 "King of the Damned", ""));
        values.add(new TestInput("The.Big.Bang.Theory.S07E23.HDTV.x264-LOL.mp4", "The Big Bang Theory", "7", "23",
                                 "The Gorilla Dissolution", ""));
        values.add(new TestInput("The.Good.Wife.S05E20.HDTV.x264-LOL.mp4", "The Good Wife", "5", "20", "The Deep Web", ""));
        values.add(new TestInput("The.Walking.Dead.S04E16.PROPER.HDTV.x264-2HD.mp4",
                                 "The Walking Dead", "4", "16", "A", ""));
        values.add(new TestInput("Veep.S03E05.HDTV.x264-KILLERS.mp4", "Veep", "3", "5", "Fishing", ""));
        values.add(new TestInput("Witches.of.East.End.S01E01.PROPER.HDTV.x264-2HD.mp4", "Witches of East End",
                                 "1", "1", "Pilot", ""));
        values.add(new TestInput("Warehouse.13.S05E04.HDTV.x264-2HD.mp4", "Warehouse 13", "5", "4", "Savage Seduction", ""));

        values.add(new TestInput("the.100.208.hdtv-lol.mp4", "The 100", "2", "8", "Spacewalker", "")); // issue #79

        values.add(new TestInput("firefly.1x01.hdtv-lol.mp4", "Firefly", "1", "1", "Serenity", ""));
        values.add(new TestInput("firefly.1x02.hdtv-lol.mp4", "Firefly", "1", "2", "The Train Job", ""));
        values.add(new TestInput("firefly.1x03.hdtv-lol.mp4", "Firefly", "1", "3", "Bushwhacked", ""));
        values.add(new TestInput("firefly.1x04.hdtv-lol.mp4", "Firefly", "1", "4", "Shindig", ""));
        values.add(new TestInput("firefly.1x05.hdtv-lol.mp4", "Firefly", "1", "5", "Safe", ""));
        values.add(new TestInput("firefly.1x06.hdtv-lol.mp4", "Firefly", "1", "6", "Our Mrs. Reynolds", ""));
        values.add(new TestInput("firefly.1x07.hdtv-lol.mp4", "Firefly", "1", "7", "Jaynestown", ""));
        values.add(new TestInput("firefly.1x08.hdtv-lol.mp4", "Firefly", "1", "8", "Out of Gas", ""));
        values.add(new TestInput("firefly.1x09.hdtv-lol.mp4", "Firefly", "1", "9", "Ariel", ""));
        values.add(new TestInput("firefly.1x10.hdtv-lol.mp4", "Firefly", "1", "10", "War Stories", ""));
        values.add(new TestInput("firefly.1x11.hdtv-lol.mp4", "Firefly", "1", "11", "Trash", ""));
        values.add(new TestInput("firefly.1x12.hdtv-lol.mp4", "Firefly", "1", "12", "The Message", ""));
        values.add(new TestInput("firefly.1x13.hdtv-lol.mp4", "Firefly", "1", "13", "Heart of Gold", ""));
        values.add(new TestInput("firefly.1x14.hdtv-lol.mp4", "Firefly", "1", "14", "Objects in Space", ""));

        values.add(new TestInput("Strike.Back.S01E01.Mini.720p.HDTV.DD5.1.x264.mkv", "Strike Back", "1", "1",
                                 "Chris Ryan's Strike Back, Episode 1", "720p"));

        values.add(new TestInput("ncis.1304.hdtv-lol", "NCIS", "13", "04", "Double Trouble", ""));

        // More tests added to test screen resolution
        values.add(new TestInput("The.Big.Bang.Theory.S10E04.720p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "The Big Bang Theory", "10", "4",
                                 "The Cohabitation Experimentation", "720p"));
        values.add(new TestInput("Lucifer.S02E03.720p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "Lucifer", "2", "3", "Sin-Eater", "720p"));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].mkv",
                                 "marvels agents of shield", "Marvel's Agents of S.H.I.E.L.D.",
                                 "4", "3", "Uprising", "1080p"));
        values.add(new TestInput("Supernatural.S11E22.1080p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "Supernatural", "11", "22", "We Happy Few", "1080p"));
        values.add(new TestInput("Supernatural.S11E22.HDTV.X264-DIMENSION.720p.[ettv].mkv",
                                 "Supernatural", "11", "22", "We Happy Few", "720p"));
        values.add(new TestInput("Channel.Zero.S01E01.480p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "Channel Zero", "1", "1", "You Have to Go Inside", "480p"));
        values.add(new TestInput("NCIS.S14E04.720p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "NCIS", "14", "4", "Love Boat", "720p"));
    }

    @Test
    public void testParseFileName() {
        for (TestInput testInput : values) {
            FileEpisode retval = new FileEpisode(testInput.input);
            assertNotNull(retval);
            TVRenamer.parseFilename(retval);
            assertEquals(testInput.input, testInput.queryString, retval.getFilenameShow());
            assertEquals(testInput.input, Integer.parseInt(testInput.season), retval.getFilenameSeason());
            assertEquals(testInput.input, Integer.parseInt(testInput.episode), retval.getFilenameEpisode());
            assertEquals(testInput.input, testInput.episodeResolution, retval.getFilenameResolution());
        }
    }

    @Test
    public void testWarehouse13() {
        FileEpisode episode = new FileEpisode("Warehouse.13.S05E04.HDTV.x264-2HD.mp4");
        assertNotNull(episode);
        TVRenamer.parseFilename(episode);
        assertEquals("warehouse 13", episode.getFilenameShow());
        assertEquals(5, episode.getFilenameSeason());
        assertEquals(4, episode.getFilenameEpisode());
    }
}
