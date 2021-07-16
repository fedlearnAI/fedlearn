#!/usr/bin/env bash

# jci 专用，开源版本需删除， jci版本所有配置文件均位于 /export/Config/ 目录下
# Usage: start.sh [-c configFile] [-d]
# -d 表示debug模式
cd $(dirname $0)
BIN_DIR=$(pwd)
cd ..
DEPLOY_DIR=$(pwd)
DEPLOY_DIR_REAL=$(readlink -f ${DEPLOY_DIR})
CONF_DIR=${DEPLOY_DIR_REAL}/conf

CONF_DIR="/export/Config/"
JAVA_DEBUG_OPTS=""
USE_DEBUG=""
while getopts ":c:d" opt; do
  case ${opt} in
  d) JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n "
    USE_DEBUG="-d" ${JAVA_DEBUG_OPTS}
    ;;
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

echo "${USE_DEBUG}"
# 根据条件判断，如果
if [ -f ${CONF_DIR}"client.properties" ]; then
  echo "client.properties"
  bash "${DEPLOY_DIR_REAL}"/bin/start-client.sh -c ${CONF_DIR}"client.properties" "${USE_DEBUG}"
elif [ -f ${CONF_DIR}"coordinator.properties" ]; then
  echo "coordinator.properties"
  bash "${DEPLOY_DIR_REAL}"/bin/start-coordinator.sh -c ${CONF_DIR}"coordinator.properties" "${USE_DEBUG}"
elif [ -f ${CONF_DIR}"manager.properties" ]; then
  echo "manager.properties"
  bash "${DEPLOY_DIR_REAL}"/bin/start-manager.sh -c ${CONF_DIR}"manager.properties" "${USE_DEBUG}"
elif [ -f ${CONF_DIR}"worker.properties" ]; then
  echo "worker.properties"
  bash "${DEPLOY_DIR_REAL}"/bin/start-worker.sh -c ${CONF_DIR}"worker.properties" "${USE_DEBUG}"
elif [ -f ${CONF_DIR}"frontend.properties" ]; then
  echo "frontend.properties"
  bash "${DEPLOY_DIR_REAL}"/bin/start-frontend.sh -c ${CONF_DIR}"frontend.properties" "${USE_DEBUG}"
else
  echo "not matched"
fi

