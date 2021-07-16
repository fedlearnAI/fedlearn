#!/usr/bin/env bash

#
# Usage: stop.sh [skip] [force [seconds_wait]]
#    skip 不dump，直接stop
#    force 最长等待seconds_wait秒，否则kill -9
#    seconds_wait 默认10，最小5
#
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
DEPLOY_DIR_REAL=`readlink -f ${DEPLOY_DIR}`
CONF_DIR=${DEPLOY_DIR_REAL}/conf

SERVER_NAME=`grep -v '^\s*#' conf/manager.properties | sed '/app.name/!d;s/.*=//' | tr -d '\r'`

if [ -z "$SERVER_NAME" ]; then
    SERVER_NAME=`hostname`
fi

PIDS=`ps -ef | grep java | grep "$CONF_DIR" |awk '{print $2}'`
if [ -z "$PIDS" ]; then
    echo "ERROR: The $SERVER_NAME does not started!"
    exit 1
fi

#if [ "$1" != "skip" ]; then
#    $BIN_DIR/dump.sh
#else
#    shift
#fi

echo "Stopping the ${SERVER_NAME}[PIDS: ${PIDS}] ..."
for PID in $PIDS ; do
    kill $PID > /dev/null 2>&1
done

MAX_WAIT=30
if [ "$1" = "force" ]; then
  if [ "$2" != "" ]; then
    MAX_WAIT=$2
  fi
fi
if [ $MAX_WAIT -lt 5 ]; then
  MAX_WAIT=5
fi
COUNT=0

while [ $COUNT -le $MAX_WAIT ]; do
    sleep 1
    ((COUNT=COUNT+1))
    for PID in $PIDS ; do
        PID_EXIST=`ps -f -p $PID | grep java`
        if [ -n "$PID_EXIST" ]; then
            if [ "$1" = "force" -a $COUNT -ge $MAX_WAIT ]; then
              echo "Force to terminate the ${SERVER_NAME}[PID: ${PID}] ..."
              kill -9 $PID
            fi
            break
        else
            ((COUNT=MAX_WAIT+1))
        fi
    done
done

echo "OK!"
echo "PID: $PIDS"

exit 0