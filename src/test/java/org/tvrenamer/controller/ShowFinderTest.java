package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.Season;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowStore;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ShowFinderTest {
    public static final List<TestInput> values = new LinkedList<>();

    @BeforeClass
    public static void setupValues() {
        values.add(new TestInput("game.of.thrones.5x01.mp4", "Game of Thrones",
                                 "5", "1", "The Wars to Come"));
        values.add(new TestInput("24.s08.e01.720p.hdtv.x264-immerse.mkv", "24",
                                 "8", "1", "Day 8: 4:00 P.M. - 5:00 P.M."));
        values.add(new TestInput("24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv", "24",
                                 "7", "18", "Day 7: 1:00 A.M. - 2:00 A.M."));
        values.add(new TestInput("human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv", "Human Target (2010)",
                                 "1", "2", "Rewind"));
        values.add(new TestInput("dexter.407.720p.hdtv.x264-sys.mkv", "Dexter",
                                 "4", "7", "Slack Tide"));
        values.add(new TestInput("JAG.S10E01.DVDRip.XviD-P0W4DVD.avi", "JAG",
                                 "10", "1", "Hail and Farewell, Part II (2)"));
        values.add(new TestInput("Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv", "Lost",
                                 "6", "5", "Lighthouse"));
        values.add(new TestInput("warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv", "Warehouse 13",
                                 "1", "1", "Pilot"));
        values.add(new TestInput("one.tree.hill.s07e14.hdtv.xvid-fqm.avi", "One Tree Hill",
                                 "7", "14", "Family Affair"));
        values.add(new TestInput("gossip.girl.s03e15.hdtv.xvid-fqm.avi", "Gossip Girl",
                                 "3", "15", "The Sixteen Year Old Virgin"));
        values.add(new TestInput("smallville.s09e14.hdtv.xvid-xii.avi", "Smallville",
                                 "9", "14", "Conspiracy"));
        values.add(new TestInput("smallville.s09e15.hdtv.xvid-2hd.avi", "Smallville",
                                 "9", "15", "Escape"));
        values.add(new TestInput("the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv", "The Big Bang Theory",
                                 "3", "18", "The Pants Alternative"));
        values.add(new TestInput("castle.2009.s01e09.720p.hdtv.x264-ctu.mkv", "Castle (2009)",
                                 "1", "9", "Little Girl Lost"));
        values.add(new TestInput("/TV/Dexter/S05E05 First Blood.mkv", "Dexter",
                                 "5", "5", "First Blood"));
        values.add(new TestInput("/TV/Lost/Lost [2x07].mkv", "Lost",
                                 "2", "7", "The Other 48 Days"));
        values.add(new TestInput("American.Dad.S09E17.HDTV.x264-2HD.mp4", "American Dad!",
                                 "9", "17", "The Full Cognitive Redaction of Avery Bullock by the Coward Stan Smith"));
        values.add(new TestInput("Californication.S07E04.HDTV.x264-2HD.mp4", "Californication",
                                 "7", "4", "Dicks"));
        values.add(new TestInput("Continuum.S03E07.HDTV.x264-2HD.mp4", "Continuum",
                                 "3", "7", "Waning Minutes"));
        values.add(new TestInput("Elementary.S02E23.HDTV.x264-LOL.mp4", "Elementary",
                                 "2", "23", "Art in the Blood"));
        values.add(new TestInput("Family.Guy.S12E19.HDTV.x264-2HD.mp4", "Family Guy",
                                 "12", "19", "Meg Stinks!"));
        values.add(new TestInput("Fargo.S01E01.HDTV.x264-2HD.mp4", "Fargo",
                                 "1", "1", "The Crocodile's Dilemma"));
        values.add(new TestInput("Girls.S03E11.HDTV.x264-KILLERS.mp4", "Girls",
                                 "3", "11", "I Saw You"));
        values.add(new TestInput("Grimm.S03E19.HDTV.x264-LOL.mp4", "Grimm",
                                 "3", "19", "Nobody Knows the Trubel I've Seen"));
        values.add(new TestInput("House.Of.Cards.2013.S01E06.HDTV.x264-EVOLVE.mp4", "House of Cards (US)",
                                 "1", "6", "Chapter 6"));
        values.add(new TestInput("Modern.Family.S05E12.HDTV.x264-EXCELLENCE.mp4", "Modern Family",
                                 "5", "12", "Under Pressure"));
        values.add(new TestInput("New.Girl.S03E23.HDTV.x264-LOL.mp4", "New Girl",
                                 "3", "23", "Cruise"));
        values.add(new TestInput("Nurse.Jackie.S06E04.HDTV.x264-2HD.mp4", "Nurse Jackie",
                                 "6", "4", "Jungle Love"));
        values.add(new TestInput("Offspring - S05E01.mp4", "Offspring",
                                 "5", "1", "Back in the Game"));
        values.add(new TestInput("Reign.2013.S01E20.HDTV.x264-2HD.mp4", "Reign (2013)",
                                 "1", "20", "Higher Ground"));
        values.add(new TestInput("Robot.Chicken.S07E04.PROPER.HDTV.x264-W4F.mp4", "Robot Chicken",
                                 "7", "4", "Rebel Appliance"));
        values.add(new TestInput("Supernatural.S09E21.HDTV.x264-LOL.mp4", "Supernatural",
                                 "9", "21", "King of the Damned"));
        values.add(new TestInput("The.Americans.2013.S02E10.HDTV.x264-LOL.mp4", "The Americans (2013)",
                                 "2", "10", "Yousaf"));
        values.add(new TestInput("The.Big.Bang.Theory.S07E23.HDTV.x264-LOL.mp4", "The Big Bang Theory",
                                 "7", "23", "The Gorilla Dissolution"));
        values.add(new TestInput("The.Good.Wife.S05E20.HDTV.x264-LOL.mp4", "The Good Wife",
                                 "5", "20", "The Deep Web"));
        values.add(new TestInput("The.Walking.Dead.S04E16.PROPER.HDTV.x264-2HD.mp4", "The Walking Dead",
                                 "4", "16", "A"));
        values.add(new TestInput("Veep.S03E05.HDTV.x264-KILLERS.mp4", "Veep",
                                 "3", "5", "Fishing"));
        values.add(new TestInput("Witches.of.East.End.S01E01.PROPER.HDTV.x264-2HD.mp4", "Witches of East End",
                                 "1", "1", "Pilot"));
        values.add(new TestInput("Warehouse.13.S05E04.HDTV.x264-2HD.mp4", "Warehouse 13",
                                 "5", "4", "Savage Seduction"));
        values.add(new TestInput("the.100.208.hdtv-lol.mp4", "The 100",
                                 "2", "8", "Spacewalker"));
        values.add(new TestInput("firefly.1x01.hdtv-lol.mp4", "Firefly",
                                 "1", "1", "The Train Job"));
        values.add(new TestInput("firefly.1x02.hdtv-lol.mp4", "Firefly",
                                 "1", "2", "Bushwhacked"));
        values.add(new TestInput("firefly.1x03.hdtv-lol.mp4", "Firefly",
                                 "1", "3", "Our Mrs. Reynolds"));
        values.add(new TestInput("firefly.1x04.hdtv-lol.mp4", "Firefly",
                                 "1", "4", "Jaynestown"));
        values.add(new TestInput("firefly.1x05.hdtv-lol.mp4", "Firefly",
                                 "1", "5", "Out of Gas"));
        values.add(new TestInput("firefly.1x06.hdtv-lol.mp4", "Firefly",
                                 "1", "6", "Shindig"));
        values.add(new TestInput("firefly.1x07.hdtv-lol.mp4", "Firefly",
                                 "1", "7", "Safe"));
        values.add(new TestInput("firefly.1x08.hdtv-lol.mp4", "Firefly",
                                 "1", "8", "Ariel"));
        values.add(new TestInput("firefly.1x09.hdtv-lol.mp4", "Firefly",
                                 "1", "9", "War Stories"));
        values.add(new TestInput("firefly.1x10.hdtv-lol.mp4", "Firefly",
                                 "1", "10", "Objects in Space"));
        values.add(new TestInput("firefly.1x11.hdtv-lol.mp4", "Firefly",
                                 "1", "11", "Serenity"));
        values.add(new TestInput("firefly.1x12.hdtv-lol.mp4", "Firefly",
                                 "1", "12", "Heart of Gold"));
        values.add(new TestInput("firefly.1x13.hdtv-lol.mp4", "Firefly",
                                 "1", "13", "Trash"));
        values.add(new TestInput("firefly.1x14.hdtv-lol.mp4", "Firefly",
                                 "1", "14", "The Message"));
        values.add(new TestInput("Strike.Back.S01E01.Mini.720p.HDTV.DD5.1.x264.mkv", "Strike Back",
                                 "1", "1", "Chris Ryan's Strike Back, Episode 1"));
        values.add(new TestInput("law.and.order.svu.1705.hdtv-lol", "Law & Order: Special Victims Unit",
                                 "17", "05", "Community Policing"));
        values.add(new TestInput("ncis.1304.hdtv-lol", "NCIS",
                                 "13", "04", "Double Trouble"));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET", "Marvel's Agents of S.H.I.E.L.D.",
                                 "3", "3", "A Wanted (Inhu)man"));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E10.HDTV.x264-KILLERS", "Marvel's Agents of S.H.I.E.L.D.",
                                 "3", "10", "Maveth"));
        values.add(new TestInput("Nip.Tuck.S06E01.720p.HDTV.X264-DIMENSION.mkv", "Nip/Tuck",
                                 "6", "1", "Don Hoberman"));
    }

    // not currently used
    private String getTitleOfFirstMatch(List<Show> shows, int season, int episode) {
        try {
            if (shows == null) {
                return "no show matches found";
            }
            if (shows.size() == 0) {
                return "zero show matches found";
            }
            Show firstShow = shows.get(0);
            if (firstShow == null) {
                return "first show is null";
            }
            Season seasObj = firstShow.getSeason(season);
            if (seasObj == null) {
                return "no season " + season + " found in " + firstShow.getName();
            }
            String title = seasObj.getTitle(episode);
            if (title == null) {
                return "no episode title found in season " + season + " of " + firstShow.getName();
            }
            return title;
        } catch (Exception e) {
            return "could not parse";
        }
    }

    private String getShowNameOfFirstMatch(List<Show> shows) {
        try {
            if (shows == null) {
                return "no show matches found";
            }
            if (shows.size() == 0) {
                return "zero show matches found";
            }
            Show firstShow = shows.get(0);
            if (firstShow == null) {
                return "first show is null";
            }
            return firstShow.getName();
        } catch (Exception e) {
            return "could not parse";
        }
    }

    @Test
    public void testDownloadShowName() {
        try {
            for (TestInput testInput : values) {
                if (testInput.episodeTitle != null) {
                    final FileEpisode fileEpisode = FilenameParser.parseFilename(testInput.input);
                    assertNotNull(fileEpisode);
                    String showName = fileEpisode.getFilenameShow();
                    final int season = fileEpisode.getFilenameSeason();
                    final int episode = fileEpisode.getFilenameEpisode();

                    final CompletableFuture<String> future = new CompletableFuture<>();
                    ShowStore.mapStringToShows(showName, new ShowInformationListener() {
                            @Override
                            public void downloadComplete(List<Show> shows) {
                                future.complete(getShowNameOfFirstMatch(shows));
                            }
                        });

                    assertEquals(testInput.show, future.get());
                }
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private static class TestInput {
        public final String input;
        public final String show;
        public final String season;
        public final String episode;

        public final String episodeTitle;

        public TestInput(String input, String show, String season, String episode) {
            this(input, show, season, episode, null);
        }

        public TestInput(String input, String show, String season, String episode, String episodeTitle) {
            this.input = input;
            this.show = show;
            this.season = season;
            this.episode = episode;

            this.episodeTitle = episodeTitle;
        }
    }
}
