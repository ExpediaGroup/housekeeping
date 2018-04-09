#!/usr/bin/env bash

if [[ -z $HOUSEKEEPING_TOOL_HOME ]]; then
  #work out the script location
  SOURCE="${BASH_SOURCE[0]}"
  while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
    SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
    SOURCE="$(readlink "$SOURCE")"
    [[ $SOURCE != /* ]] && SOURCE="$SCRIPT_DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
  done
  HOUSEKEEPING_TOOL_HOME="$( cd -P "$( dirname "$SOURCE" )" && cd .. && pwd )"
fi

echo "Using Housekeeping Home $HOUSEKEEPING_TOOL_HOME"

if [[ -z ${HIVE_LIB-} ]]; then
  export HIVE_LIB=/usr/hdp/current/hive-client/lib
fi
if [[ -z ${HCAT_LIB-} ]]; then
  export HCAT_LIB=/usr/hdp/current/hive-webhcat/share/hcatalog
fi
if [[ -z ${HIVE_CONF_PATH-} ]]; then
  export HIVE_CONF_PATH=/etc/hive/conf
fi

LIBFB303_JAR=`ls $HIVE_LIB/libfb303-*.jar | tr '\n' ':'`

HOUSEKEEPING_LIBS=$HOUSEKEEPING_TOOL_HOME/lib/*:$HIVE_LIB/hive-exec.jar:$HIVE_LIB/hive-metastore.jar:$LIBFB303_JAR:$HIVE_CONF_PATH

if [[ -z ${HOUSEKEEPING_CLASSPATH-} ]]; then
  export HOUSEKEEPING_CLASSPATH=$HOUSEKEEPING_LIBS
else
  export HOUSEKEEPING_CLASSPATH=$HOUSEKEEPING_CLASSPATH:$HOUSEKEEPING_LIBS
fi

if [[ -z ${HADOOP_CLASSPATH-} ]]; then
  export HADOOP_CLASSPATH=$HOUSEKEEPING_CLASSPATH
else
  export HADOOP_CLASSPATH=$HOUSEKEEPING_CLASSPATH:$HADOOP_CLASSPATH
fi
