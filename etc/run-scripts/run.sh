#!/bin/bash

# run.sh -- in theory, detects your platform and runs the right script.
#  In practice, used to redirect run-mingw to a logfile, without bothering
#  to learn how to properly configure the logger.

# logdir -- change this if you want the logfile written elsewhere
logdir=~/Documents/Logs

scriptsdir=`dirname $0`
projdir=${scriptsdir}/../..

cd ${projdir}
ant clean

# The program uses java.util.logging, which apparently writes to stderr
# by default.  The program has a logging configuration, which makes the
# output go to stdout, instead.  But it doesn't always find its logging
# configuration.  To make sure to send all output to the logfile, redirect
# stderr into stdout, and stdout to the logfile.
${scriptsdir}/run-mingw.sh $* > ${logdir}/tvrenamer.log
