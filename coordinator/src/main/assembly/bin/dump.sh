#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CONF_DIR=$DEPLOY_DIR/conf

SERVER_NAME=`grep -v '^\s*#' conf/master.properties | sed '/master.name/!d;s/.*=//' | tr -d '\r'`
APP=`sed '/"APP"/!d;s/.*value="\(.*\)".*/\1/' conf/logback.xml`
eval LOGS_DIR=`sed '/"LOG_HOME"/!d;s/.*value="\(.*\)".*/\1/' conf/logback.xml`

if [ -z "$APP" ]; then
  echo "ERROR: Not Found APP defined in conf/logback.xml"
  exit 1
fi

if [ -z "$LOGS_DIR" ]; then
  echo "ERROR: Not Found LOG_HOME defined in conf/logback.xml"
  exit 1
fi

if [ -z "$SERVER_NAME" ]; then
#    SERVER_NAME=`hostname`
    SERVER_NAME=$APP
fi

PIDS=`ps -ef | grep java | grep "$CONF_DIR" |awk '{print $2}'`
if [ -z "$PIDS" ]; then
    echo "ERROR: The $SERVER_NAME does not started!"
    exit 1
fi

if test -n "${JAVA_HOME}"; then
  JSTACK=$JAVA_HOME/bin/jstack
  JINFO=$JAVA_HOME/bin/jinfo
  JSTAT=$JAVA_HOME/bin/jstat
  JMAP=$JAVA_HOME/bin/jmap
else
  JSTACK=jstack
  JINFO=jinfo
  JSTAT=jstat
  JMAP=jmap
fi

if [ -z "$LOGS_DIR" ]; then
    LOGS_DIR=$DEPLOY_DIR/logs
fi
if [ ! -d $LOGS_DIR ]; then
    mkdir $LOGS_DIR
fi
DUMP_DIR=$LOGS_DIR/dump
if [ ! -d $DUMP_DIR ]; then
    mkdir $DUMP_DIR
fi
DUMP_DATE=`date +%Y%m%d%H%M%S`
DATE_DIR=$DUMP_DIR/$DUMP_DATE
if [ ! -d $DATE_DIR ]; then
    mkdir $DATE_DIR
fi
if [ ! -d $DATE_DIR ]; then
  echo "ERROR: Please check dump directory '$DATE_DIR' is ok?"
  exit 1
fi

echo "Dumping the $SERVER_NAME[PIDS: $PIDS] ..."
for PID in $PIDS ; do
    echo "Dumping jstack $PID"
    ${JSTACK} $PID > $DATE_DIR/jstack-$PID.dump 2>&1
    echo "Dumping jinfo $PID"
    ${JINFO} $PID > $DATE_DIR/jinfo-$PID.dump 2>&1
    echo "Dumping jstat -gcutil $PID"
    ${JSTAT} -gcutil $PID > $DATE_DIR/jstat-gcutil-$PID.dump 2>&1
    echo "Dumping jstat -gccapacity $PID"
    ${JSTAT} -gccapacity $PID > $DATE_DIR/jstat-gccapacity-$PID.dump 2>&1
    echo "Dumping jmap $PID"
    ${JMAP} $PID > $DATE_DIR/jmap-$PID.dump 2>&1
    echo "Dumping jmap -heap $PID"
    ${JMAP} -heap $PID > $DATE_DIR/jmap-heap-$PID.dump 2>&1
    echo "Dumping jmap -histo $PID"
    ${JMAP} -histo $PID > $DATE_DIR/jmap-histo-$PID.dump 2>&1
    if [ -r /usr/sbin/lsof ]; then
      echo "Dumping lsof -p $PID"
      /usr/sbin/lsof -p $PID > $DATE_DIR/lsof-$PID.dump
    fi
done

if [ -r /bin/netstat ]; then
echo "Dumping netstat"
/bin/netstat -an > $DATE_DIR/netstat.dump 2>&1
fi
if [ -r /usr/bin/iostat ]; then
echo "Dumping iostat"
/usr/bin/iostat > $DATE_DIR/iostat.dump 2>&1
fi
if [ -r /usr/bin/mpstat ]; then
echo "Dumping mpstat"
/usr/bin/mpstat > $DATE_DIR/mpstat.dump 2>&1
fi
if [ -r /usr/bin/vmstat ]; then
echo "Dumping vmstat"
/usr/bin/vmstat > $DATE_DIR/vmstat.dump 2>&1
fi
if [ -r /usr/bin/free ]; then
echo "Dumping free"
/usr/bin/free -t > $DATE_DIR/free.dump 2>&1
fi
if [ -r /usr/bin/sar ]; then
echo "Dumping sar"
/usr/bin/sar > $DATE_DIR/sar.dump 2>&1
fi
if [ -r /usr/bin/uptime ]; then
echo "Dumping uptime"
/usr/bin/uptime > $DATE_DIR/uptime.dump 2>&1
fi

echo "OK!"
echo "DUMP: $DATE_DIR"
