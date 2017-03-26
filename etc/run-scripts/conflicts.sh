#!/bin/bash

indir=~/Movies/intest
outdir=~/Movies/outtest
runprog=run-osx.sh

if [ "`uname`" != "Darwin" ]
then
  indir=~/Desktop/testdir/intest
  outdir=~/Desktop/testdir/outtest
  runprog=run-mingw.sh
fi

/bin/rm -rf ${indir}
/bin/rm -rf ${outdir}

/bin/mkdir ${indir}
echo contents > ${indir}/Dilbert.S01E09.The.Knack.avi
echo contents > ${indir}/Futurama.S07E01.The.Bots.and.the.Bees.avi
echo contents > ${indir}/Transparent.S02E08.Oscillate.mp4

/bin/mkdir ${indir}/Frasier
echo contents > ${indir}/Frasier/Frasier.S10E01.The.Ring.Cycle.avi
echo contents > ${indir}/Frasier/Frasier.S10E01.dvdrip.avi
/bin/mkdir ${indir}/Veep
echo contents1234 > ${indir}/Veep.S04E07.Mommy.Meyer.2015.05.24~6.mp4
echo contents12345 > ${indir}/Veep/Veep.S04E07.Mommy.Meyer.mp4
echo contents123 > ${indir}/Veep/Veep.S04E07.Mommy.Meyer.2015.05.24~2.mp4
echo contents123456 > ${indir}/Veep/Veep.S04E07.Mommy.Meyer.2015.05.24~4.mp4
echo contents > ${indir}/Veep/Veep.S04E07.Mommy.Meyer.2015.05.24~5.mp4
echo contents1 > ${indir}/Veep/Veep.S04E07.Mommy.Meyer.2015.05.24~7.mp4
echo contents12 > ${indir}/Veep/Veep.S04E07.Mommy.Meyer.2015.05.24~11.mp4

cd ~/Documents/VC/tvrenamer
./etc/run-scripts/${runprog} -build || exit 1

echo
echo '** indir'
/bin/ls ${indir}


echo
echo '** outdir'
/bin/ls -lR ${outdir}
