#!/bin/bash

indir=~/Desktop/Videos/testdir/intest
outdir=~/Desktop/Videos/testdir/outtest
runprog=run-mingw.sh

if [ "`uname`" = "Darwin" ]
then
  indir=~/Movies/FakeFiles/intest
  outdir=~/Movies/FakeFiles/outtest
  runprog=run-osx.sh
fi

clean=no

/bin/rm -rf ${indir}
/bin/rm -rf ${outdir}

/bin/mkdir ${indir}
echo contents > ${indir}/Bewitched.S05E02.Samantha.Goes.South.for.a.Spell.avi
echo contents > ${indir}/Community.S04E11.Basic.Human.Anatomy.mkv
echo contents > ${indir}/Dilbert.S01E09.The.Knack.avi
echo contents > ${indir}/Frasier.S10E01.The.Ring.Cycle.avi
echo contents > ${indir}/Frasier.S10E01.dvdrip.avi
echo contents > ${indir}/Friends.S09E01.The.One.Where.No.One.Proposes.avi
echo contents > ${indir}/Futurama.S07E01.The.Bots.and.the.Bees.avi
echo contents > ${indir}/Glee.S06E07.Transitioning.m4v
echo contents > ${indir}/Mom.S03E11.Cinderella.and.a.Drunk.MacGyver.mp4
echo contents > ${indir}/Seinfeld.S08E01.The.Foundation.mkv
echo contents > ${indir}/Transparent.S02E08.Oscillate.mp4
echo contents > ${indir}/Veep.S04E07.Mommy.Meyer.mp4

/bin/mkdir ${indir}/Futurama
echo contents > ${indir}/Futurama/S07E01.The.Bots.and.the.Bees.avi

/bin/mkdir ${indir}/JohnLarroquetteShow
/bin/mkdir "${indir}/JohnLarroquetteShow/Season 3"
echo contents > "${indir}/JohnLarroquetteShow/Season 3/3x18 - The Dance.avi"

# echo contents > "${indir}/Beavis.and.Butt-head.S03E11.x264-FLEET.avi"
# echo contents > ${indir}/MythBusters.2007x14.mpg

/bin/mkdir ${outdir}
/bin/mkdir ${outdir}/Veep
echo contents > ${outdir}/Veep/Veep.S04E07.Mommy.Meyer.2015.05.24.mp4

cd ~/Documents/VC/tvrenamer
./etc/run-scripts/${runprog} -build || exit 1

echo '** outdir'
/bin/ls ${outdir}

echo '** outdir/Veep'
/bin/ls ${outdir}/Veep

echo
echo '** indir'
/bin/ls ${indir}
