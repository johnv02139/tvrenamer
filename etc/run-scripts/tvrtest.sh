#!/bin/bash

prefs=~/.tvrenamer/prefs.xml

srcdir=~/Documents/VC/tvrenamer

indir=~/Desktop/Videos/testdir/intest
outdir=~/Desktop/Videos/testdir/outtest
runprog=run-mingw.sh

if [ "`uname`" = "Darwin" ]
then
  indir=~/Movies/FakeFiles/intest
  outdir=~/Movies/FakeFiles/outtest
  runprog=run-osx.sh
elif [ "`uname`" = "Linux" ]
then
  indir=~/Videos/intest
  outdir=~/Videos/outtest
  runprog=run-ubuntu.sh
fi

clean=no
testtoo=no
swap=no

if [ "$1" = "-test" ]
then
  testtoo=yes
fi

if [ "${swap}" = "yes" ]
then
  sed 's,destDir,tempname,g' ${prefs} > ~/x
  sed 's,preloadFolder,destDir,g' ~/x > ~/y
  sed 's,tempname,preloadFolder,g' ~/y > ${prefs}
  /bin/rm ~/x ~/y
fi

/bin/rm -rf ${indir}
/bin/rm -rf ${outdir}

decache ()
{
  if [ -n "$1" ]
  then
    cf=~/.tvrenamer/thetvdb/${1}.xml
    echo $cf
    /bin/rm -f $cf
  fi
}

# decache veep
# decache 237831

if [ "${clean}" = "yes" ]
then
  decache bewitched
  decache dilbert
  decache frasier
  decache friends
  decache futurama
  decache glee
  decache mom
  decache seinfeld
  decache transparent
  decache 71528
  decache 94571
  decache 78581
  decache 77811
  decache 79168
  decache 73871
  decache 83610
  decache 266967
  decache 79169
  decache 278334
fi

/bin/mkdir -p ${indir}
# getShowNamePlaceholder() (temporarily)
echo contents > "${indir}/Dilbert.S01E08.avi"

# getNoMatchPlaceholder()
echo contents > "${indir}/Dilbert.S01E087.avi"

# single match
echo contents > "${indir}/Bewitched.S05E02.Samantha.Goes.South.for.a.Spell.avi"

# multiple matches
echo contents > "${indir}/Dilbert S01E09 The Knack.avi"

# miniseries -- no season, just episode
echo contents > "${indir}/Lonesome Dove 02.mkv"

# series hacked to fail to get listings
echo contents > "${indir}/Outsourced.S01E11.mp4"

# ignored series
echo contents > "${indir}/Quintuplets.S01E11.mp4"

# more multiple episodes
echo contents > "${indir}/RobotChicken.S08E07.avi"
echo contents > "${indir}/RobotChicken.S08E07.Joel.avi"
echo contents > "${indir}/Robot Chicken - S08E13.avi"
echo contents > "${indir}/Robot Chicken - S08E13.nfo"
echo contents > "${indir}/Robot.Chicken.S08E14.avi"

# ADDED_PLACEHOLDER_FILENAME
# getNoShowPlaceholder()
echo contents > "${indir}/Schmandy Larker, M.D. S01E04.mp4"

# BAD_PARSE_MESSAGE
echo contents > "${indir}/Casablanca.mp4"

# BAD_PARSE_MESSAGE for got listings but no show -- can't happen

# getNoListingsPlaceholder()
# can hack to test; can come if listingsFailed or if listingsSucceeded but there aren't any

# echo contents > "${indir}/the.big.bang.theory.1020.hdtv-lol.mp4"

/bin/mkdir ${indir}/Futurama
# /bin/mkdir ${indir}/Undeclared
echo contents > "${indir}/Futurama/Futurama.S04E10.avi"

# echo contents > "${indir}/Futurama/S02E04 - Xmas Story.avi"
# echo contents > "${indir}/Futurama/S02E11 - How Hermes Requisitioned His Groove Back.avi"
# echo contents > "${indir}/Futurama/S02E15 - The Problem With Popplers.avi"
# echo contents > "${indir}/Undeclared/S01E03 - Eric Visits.avi"
# echo contents > "${indir}/Undeclared/S01E04 - Jobs, Jobs, Jobs.avi"
# echo contents > "${indir}/Undeclared/S01E05 - Sick in the Head.avi"
# echo contents > "${indir}/Undeclared/S01E06 - The Assistant.avi"
# echo contents > "${indir}/Undeclared/S01E14 - The Day After.avi"
# echo contents > "${indir}/Undeclared/S01E15 - The Perfect Date.avi"
# echo contents > "${indir}/Undeclared/S01E16 - Hal and Hilary.avi"
# echo contents > "${indir}/Undeclared/S01E17 - Eric's POV.avi"

cd ${srcdir}
ant clean compile

if [ "${testtoo}" = "yes" ]
then
  ant test&
fi

./etc/run-scripts/${runprog} -build || exit 1

echo '** outdir'
/bin/ls -R ${outdir}

echo '** outdir/Futurama'
/bin/ls ${outdir}/Futurama

echo
echo '** indir'
/bin/ls -R ${indir}
