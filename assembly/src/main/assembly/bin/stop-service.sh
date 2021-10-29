#!/usr/bin/env bash

#
# Usage: stop.sh [skip] [force [seconds_wait]]
#    skip 不dump，直接stop
#    force 最长等待seconds_wait秒，否则kill -9
#    seconds_wait 默认10，最小5
#
SERVER_NAME=""
echo $SERVER_NAME
JAVA_DEBUG_OPTS=""
while getopts ":s:f" opt; do
  case ${opt} in
  f) JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n "
    USE_DEBUG="-d" ${JAVA_DEBUG_OPTS}
    ;;
  s) SERVER_NAME=$OPTARG;;
  :) # 没有为需要参数的选项指定参数
    echo "This option -$OPTARG requires an argument."
    exit 1
    ;;
  ?) # 发现了无效的选项
    echo "-$OPTARG is not an option"
    exit 2
    ;;
  esac
done

echo $SERVER_NAME

#if [ -z "$SERVER_NAME" ]; then
#    SERVER_NAME=`hostname`
#fi

PIDS=`ps -ef | grep java | grep "$SERVER_NAME" |awk '{print $2}'`
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
# 如果第一个参数时force，判断第二个参数是否是空，不是空的话，将等待值设为第二个参数，否则默认为5
#MAX_WAIT=10
#if [ "$1" = "force" ]; then
#  if [ "$2" != "" ]; then
#    MAX_WAIT=$2
#  fi
#fi
#if [ $MAX_WAIT -lt 5 ]; then
#  MAX_WAIT=5
#fi
#COUNT=0
#
#while [ $COUNT -le $MAX_WAIT ]; do
#    sleep 1
#    ((COUNT=COUNT+1))
#    for PID in $PIDS ; do
#        PID_EXIST=`ps -f -p $PID | grep java`
#        if [ -n "$PID_EXIST" ]; then
#            if [ "$1" = "force" -a $COUNT -ge $MAX_WAIT ]; then
#              echo "Force to terminate the ${SERVER_NAME}[PID: ${PID}] ..."
#              kill -9 $PID
#            fi
#            break
#        else
#            ((COUNT=MAX_WAIT+1))
#        fi
#    done
#done

echo "OK!"
echo "PID: $PIDS"

exit 0
