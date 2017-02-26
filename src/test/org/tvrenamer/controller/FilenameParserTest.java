package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.tvrenamer.controller.FilenameParser.COMPILED_REGEX;
import static org.tvrenamer.controller.FilenameParser.removeLast;
import static org.tvrenamer.controller.FilenameParser.stripJunk;

import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.ShowStore;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilenameParserTest {

    private static class TestInput {
        public final String input;
        public final String queryString;
        public final String season;
        public final String episode;
        public final String episodeResolution;

        public TestInput(String input, String queryString, String season, String episode) {
            this(input, queryString, season, episode, "");
        }

        public TestInput(String input, String queryString,
                         String season, String episode, String episodeResolution)
        {
            this.input = input;
            this.queryString = queryString;
            this.season = season;
            this.episode = episode;
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
        // (3) the season number, as a string
        // (4) the episode number, as a string
        // (5) the screen resolution found in the filename
        values.add(new TestInput("Nip.Tuck.S06E01.720p.HDTV.X264-DIMENSION.mkv",
                                 "nip tuck", "6", "1", "720p"));
        values.add(new TestInput("human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv",
                                 "human target 2010", "1", "2", "720p"));
        values.add(new TestInput("castle.2009.s01e09.720p.hdtv.x264-ctu.mkv",
                                 "castle 2009", "1", "9", "720p"));
        values.add(new TestInput("24.s08.e01.720p.hdtv.x264-immerse.mkv", "24", "8", "1", "720p"));
        values.add(new TestInput("24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv", "24", "7", "18", "720p"));
        values.add(new TestInput("dexter.407.720p.hdtv.x264-sys.mkv", "dexter", "4", "7", "720p"));
        values.add(new TestInput("Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv",
                                 "lost", "6", "5", "720p"));
        values.add(new TestInput("warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv",
                                 "warehouse 13", "1", "1", "720p"));
        values.add(new TestInput("the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv",
                                 "the big bang theory", "3", "18", "720p"));
        values.add(new TestInput("Strike.Back.S01E01.Mini.720p.HDTV.DD5.1.x264.mkv",
                                 "strike back", "1", "1", "720p"));
        values.add(new TestInput("The.Big.Bang.Theory.S10E04.720p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "the big bang theory", "10", "4", "720p"));
        values.add(new TestInput("Lucifer.S02E03.720p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "lucifer", "2", "3", "720p"));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].mkv",
                                 "marvels agents of shield", "4", "3", "1080p"));
        values.add(new TestInput("Supernatural.S11E22.1080p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "supernatural", "11", "22", "1080p"));
        values.add(new TestInput("Supernatural.S11E22.HDTV.X264-DIMENSION.720p.[ettv].mkv",
                                 "supernatural", "11", "22", "720p"));
        values.add(new TestInput("Channel.Zero.S01E01.480p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "channel zero", "1", "1", "480p"));
        values.add(new TestInput("NCIS.S14E04.720p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "ncis", "14", "4", "720p"));



        // In this section, we do not supply an explicit query string.  The query string is simply
        //  the show name, lower-cased. So, we have:
        // (1) actual filename to test
        // (2) the official name of the show we expect to match, as we expect the provider to return it,
        //     which, after being lower-cased, is also what we expect to pull out of the filename
        // (3) the season number, as a (non-padded) string
        // (4) the episode number, as a (non-padded) string
        // (5) the screen resolution found in the filename
        values.add(new TestInput("American.Dad.S09E17.HDTV.x264-2HD.mp4",
                                 "american dad", "9", "17"));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET",
                                 "marvels agents of shield", "3", "3"));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E10.HDTV.x264-KILLERS",
                                 "marvels agents of shield", "3", "10"));
        values.add(new TestInput("Reign.2013.S01E20.HDTV.x264-2HD.mp4",
                                 "reign 2013", "1", "20"));
        values.add(new TestInput("The.Americans.2013.S02E10.HDTV.x264-LOL.mp4",
                                 "the americans 2013", "2", "10"));
        values.add(new TestInput("law.and.order.svu.1705.hdtv-lol",
                                 "law and order svu", "17", "05"));
        values.add(new TestInput("House.Of.Cards.2013.S01E06.HDTV.x264-EVOLVE.mp4",
                                 "house of cards 2013", "1", "6"));


        values.add(new TestInput("game.of.thrones.5x01.mp4", "game of thrones", "5", "1"));
        values.add(new TestInput("JAG.S10E01.DVDRip.XviD-P0W4DVD.avi", "jag", "10", "1"));
        values.add(new TestInput("one.tree.hill.s07e14.hdtv.xvid-fqm.avi", "one tree hill", "7", "14"));
        values.add(new TestInput("gossip.girl.s03e15.hdtv.xvid-fqm.avi", "gossip girl", "3", "15"));
        values.add(new TestInput("smallville.s09e14.hdtv.xvid-xii.avi", "smallville", "9", "14"));
        values.add(new TestInput("smallville.s09e15.hdtv.xvid-2hd.avi", "smallville", "9", "15"));
        values.add(new TestInput("/TV/Dexter/S05E05 First Blood.mkv", "dexter", "5", "5"));
        values.add(new TestInput("/TV/Lost/Lost [2x07].mkv", "lost", "2", "7"));
        values.add(new TestInput("Californication.S07E04.HDTV.x264-2HD.mp4", "californication", "7", "4"));
        values.add(new TestInput("Continuum.S03E07.HDTV.x264-2HD.mp4", "continuum", "3", "7"));
        values.add(new TestInput("Elementary.S02E23.HDTV.x264-LOL.mp4", "elementary", "2", "23"));
        values.add(new TestInput("Family.Guy.S12E19.HDTV.x264-2HD.mp4", "family guy", "12", "19"));
        values.add(new TestInput("Fargo.S01E01.HDTV.x264-2HD.mp4", "fargo", "1", "1"));
        values.add(new TestInput("Girls.S03E11.HDTV.x264-KILLERS.mp4", "girls", "3", "11"));
        values.add(new TestInput("Grimm.S03E19.HDTV.x264-LOL.mp4", "grimm", "3", "19"));
        values.add(new TestInput("Modern.Family.S05E12.HDTV.x264-EXCELLENCE.mp4",
                                 "modern family", "5", "12"));
        values.add(new TestInput("New.Girl.S03E23.HDTV.x264-LOL.mp4", "new girl", "3", "23"));
        values.add(new TestInput("Nurse.Jackie.S06E04.HDTV.x264-2HD.mp4", "nurse jackie", "6", "4"));
        values.add(new TestInput("Offspring - S05E01.mp4", "offspring", "5", "1"));
        values.add(new TestInput("Robot.Chicken.S07E04.PROPER.HDTV.x264-W4F.mp4",
                                 "robot chicken", "7", "4"));
        values.add(new TestInput("Supernatural.S09E21.HDTV.x264-LOL.mp4", "supernatural", "9", "21"));
        values.add(new TestInput("The.Big.Bang.Theory.S07E23.HDTV.x264-LOL.mp4",
                                 "the big bang theory", "7", "23"));
        values.add(new TestInput("The.Good.Wife.S05E20.HDTV.x264-LOL.mp4", "the good wife", "5", "20"));
        values.add(new TestInput("The.Walking.Dead.S04E16.PROPER.HDTV.x264-2HD.mp4",
                                 "the walking dead", "4", "16"));
        values.add(new TestInput("Veep.S03E05.HDTV.x264-KILLERS.mp4", "veep", "3", "5"));
        values.add(new TestInput("Witches.of.East.End.S01E01.PROPER.HDTV.x264-2HD.mp4",
                                 "witches of east end", "1", "1"));
        values.add(new TestInput("Warehouse.13.S05E04.HDTV.x264-2HD.mp4", "warehouse 13", "5", "4"));
        values.add(new TestInput("the.100.208.hdtv-lol.mp4", "the 100", "2", "8")); // issue #79
        values.add(new TestInput("firefly.1x01.hdtv-lol.mp4", "firefly", "1", "1"));
        values.add(new TestInput("firefly.1x02.hdtv-lol.mp4", "firefly", "1", "2"));
        values.add(new TestInput("firefly.1x03.hdtv-lol.mp4", "firefly", "1", "3"));
        values.add(new TestInput("firefly.1x04.hdtv-lol.mp4", "firefly", "1", "4"));
        values.add(new TestInput("firefly.1x05.hdtv-lol.mp4", "firefly", "1", "5"));
        values.add(new TestInput("firefly.1x06.hdtv-lol.mp4", "firefly", "1", "6"));
        values.add(new TestInput("firefly.1x07.hdtv-lol.mp4", "firefly", "1", "7"));
        values.add(new TestInput("firefly.1x08.hdtv-lol.mp4", "firefly", "1", "8"));
        values.add(new TestInput("firefly.1x09.hdtv-lol.mp4", "firefly", "1", "9"));
        values.add(new TestInput("firefly.1x10.hdtv-lol.mp4", "firefly", "1", "10"));
        values.add(new TestInput("firefly.1x11.hdtv-lol.mp4", "firefly", "1", "11"));
        values.add(new TestInput("firefly.1x12.hdtv-lol.mp4", "firefly", "1", "12"));
        values.add(new TestInput("firefly.1x13.hdtv-lol.mp4", "firefly", "1", "13"));
        values.add(new TestInput("firefly.1x14.hdtv-lol.mp4", "firefly", "1", "14"));
        values.add(new TestInput("ncis.1304.hdtv-lol", "ncis", "13", "04"));
    }

    @Test
    public void testRemoveLast() {
        // Straighforward removal; note does not remove punctuation/separators
        assertEquals("foo..baz", removeLast("foo.bar.baz", "bar"));

        // Implementation detail, but the match is required to be all lower-case,
        // while the input doesn't
        assertEquals("Foo..Baz", removeLast("Foo.Bar.Baz", "bar"));

        // Like the name says, the method only removes the last instance
        assertEquals("bar.foo..baz", removeLast("bar.foo.bar.baz", "bar"));

        // Doesn't have to be delimited
        assertEquals("emassment", removeLast("embarassment", "bar"));

        // Doesn't necessarily replace anything
        assertEquals("Foo.Schmar.baz", removeLast("Foo.Schmar.baz", "bar"));

        // This frankly is probably a bug, but this is currently the expected behavior.
        // If the match is not all lower-case to begin with, nothing will be matched.
        assertEquals("Foo.Bar.Baz", removeLast("Foo.Bar.Baz", "Bar"));
    }

    @Test
    public void testStripJunk() {
        // Straighforward removal; note does not remove punctuation/separators
        assertEquals("foo..baz", stripJunk("foo.dvdrip.baz"));

        // Implementation detail, but the match is required to be all lower-case,
        // while the input doesn't
        assertEquals("Foo..Baz", stripJunk("Foo.Dvdrip.Baz"));

        // Like the name says, the method only removes the last instance
        assertEquals("dvdrip.foo..baz", stripJunk("dvdrip.foo.dvdrip.baz"));

        // Doesn't have to be delimited
        assertEquals("emassment", stripJunk("emdvdripassment"));

        // Doesn't necessarily replace anything
        assertEquals("Foo.Schmar.baz", stripJunk("Foo.Schmar.baz"));
    }

    private void testInsertingShowName(String filepath, String expected) {
        File file = new File(filepath);
        String actual = FilenameParser.insertShowNameIfNeeded(file);
        assertEquals(expected, actual);
    }

    @Test
    public void testInsertShowNameIfNeeded() {
        testInsertingShowName("Nip.Tuck.S06E01.720p.HDTV.X264-DIMENSION.mkv",
                              "Nip.Tuck.S06E01.720p.HDTV.X264-DIMENSION.mkv");
        testInsertingShowName("/TV/Dexter/S05E05 First Blood.mkv",
                              "Dexter S05E05 First Blood.mkv");
        testInsertingShowName("/TV/Lost/Lost [2x07].mkv",
                              "Lost [2x07].mkv");
        testInsertingShowName("/Sitcoms/Veep/S04/S4E3 Data.api",
                              "Veep S4E3 Data.api");
        testInsertingShowName("/Sitcoms/Veep/Season 4/S04E03 Data.api",
                              "Veep S04E03 Data.api");
        testInsertingShowName("/Drama/24/S02/S02E18 Day 2: 1:00 A.M. - 2:00 A.M..api",
                              "24 S02E18 Day 2: 1:00 A.M. - 2:00 A.M..api");
        testInsertingShowName("/Animation/AmericanDad/S11/S11E1 Roger Passes the Bar.mp4",
                              "AmericanDad S11E1 Roger Passes the Bar.mp4");
    }

    private void testRegexParse(Pattern regex, String input, String show,
                                String season, String episode, String resolution)
    {
        Matcher matcher = regex.matcher(input);
        if (matcher.matches()) {
            assertEquals(show, matcher.group(1));
            assertEquals(season, matcher.group(2));
            assertEquals(episode, matcher.group(3));
            if (matcher.groupCount() == 4) {
                assertEquals(resolution, matcher.group(4));
            } else if (matcher.groupCount() == 3) {
                assertNull(resolution);
            } else {
                fail("Matched but wrong number of groups: " + matcher.groupCount()
                     + " on input " + input);
            }
        } else if ((show == null) && (season == null)
                   && (resolution == null))
        {
            assertNull(episode);
        } else {
            fail("could not parse " + input);
        }
    }

    private void testRegexUnparseable(Pattern regex, String input) {
        Matcher matcher = regex.matcher(input);
        if (matcher.matches()) {
            fail("expected not to be able to parse " + input);
        } else {
            assertNotNull(input);
        }
    }

    @Test
    public void testCompiledRegex() {
        testRegexParse(COMPILED_REGEX[0], "warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv",
                       "warehouse.13.", "1", "01", "720p");
        testRegexParse(COMPILED_REGEX[1], "24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv",
                       "24.", "07", "18", "720p");
        testRegexParse(COMPILED_REGEX[2], "human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv",
                       "human.target.2010.", "01", "02", "720p");
        testRegexParse(COMPILED_REGEX[1], "human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv",
                       "human.target.2010.", "01", "02", "720p");
        testRegexParse(COMPILED_REGEX[3], "game.of.thrones.5x01.1080p.mp4",
                       "game.of.thrones.", "5", "01", "1080p");
        testRegexParse(COMPILED_REGEX[4], "dexter.407.720p.hdtv.x264-sys.mkv",
                       "dexter.", "4", "07", "720p");

        testRegexParse(COMPILED_REGEX[5], "American.Dad.S09E17.HDTV.x264-2HD.mp4",
                       "American.Dad.", "09", "17", null);
        testRegexParse(COMPILED_REGEX[6], "24.s08.e01.hdtv.x264-immerse.mkv",
                       "24.", "08", "01", null);
        testRegexParse(COMPILED_REGEX[7], "House.Of.Cards.2013.S01E06.HDTV.x264-EVOLVE.mp4",
                       "House.Of.Cards.2013.", "01", "06", null);
        testRegexParse(COMPILED_REGEX[8], "/TV/Lost/Lost [2x07].mkv",
                       "/TV/Lost/Lost [", "2", "07", null);
        testRegexParse(COMPILED_REGEX[9], "ncis.1304.hdtv-lol",
                       "ncis.", "13", "04", null);

        testRegexParse(COMPILED_REGEX[5], "warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv",
                       "warehouse.13.", "1", "01", null);
        testRegexParse(COMPILED_REGEX[6], "24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv",
                       "24.", "07", "18", null);
        testRegexParse(COMPILED_REGEX[7], "human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv",
                       "human.target.2010.", "01", "02", null);
        testRegexParse(COMPILED_REGEX[8], "game.of.thrones.5x01.1080p.mp4",
                       "game.of.thrones.", "5", "01", null);
        testRegexParse(COMPILED_REGEX[9], "dexter.407.hdtv.x264-sys.mkv",
                       "dexter.", "4", "07", null);
        // This is clearly wrong.  But add it as a test to document the current behavior.
        testRegexParse(COMPILED_REGEX[9], "dexter.407.720p.hdtv.x264-sys.mkv",
                       "dexter.407.", "7", "20", null);

        testRegexUnparseable(COMPILED_REGEX[0], "24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv");
        testRegexUnparseable(COMPILED_REGEX[0], "human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv");
        testRegexUnparseable(COMPILED_REGEX[0], "game.of.thrones.5x01.1080p.mp4");
        testRegexUnparseable(COMPILED_REGEX[1], "game.of.thrones.5x01.1080p.mp4");
        testRegexUnparseable(COMPILED_REGEX[2], "game.of.thrones.5x01.1080p.mp4");
        testRegexUnparseable(COMPILED_REGEX[0], "dexter.407.720p.hdtv.x264-sys.mkv");
        testRegexUnparseable(COMPILED_REGEX[1], "dexter.407.720p.hdtv.x264-sys.mkv");
        testRegexUnparseable(COMPILED_REGEX[2], "dexter.407.720p.hdtv.x264-sys.mkv");
        testRegexUnparseable(COMPILED_REGEX[3], "dexter.407.720p.hdtv.x264-sys.mkv");
    }

    @Test
    public void testParseFileName() {
        for (TestInput testInput : values) {
            FileEpisode retval = new FileEpisode(testInput.input);
            assertTrue(retval.wasParsed());
            assertEquals(testInput.input, testInput.queryString,
                         ShowStore.makeQueryString(retval.getFilenameSeries()));
            assertEquals(testInput.input,
                         Integer.parseInt(testInput.season),
                         Integer.parseInt(retval.getFilenameSeason()));
            assertEquals(testInput.input,
                         Integer.parseInt(testInput.episode),
                         Integer.parseInt(retval.getFilenameEpisode()));
            assertEquals(testInput.input, testInput.episodeResolution, retval.getFilenameResolution());
        }
    }

    @Test
    public void testWarehouse13() {
        FileEpisode episode = new FileEpisode("Warehouse.13.S05E04.HDTV.x264-2HD.mp4");
        assertTrue(episode.wasParsed());
        assertEquals("warehouse 13", ShowStore.makeQueryString(episode.getFilenameSeries()));
        assertEquals("05", episode.getFilenameSeason());
        assertEquals("04", episode.getFilenameEpisode());
    }
}
