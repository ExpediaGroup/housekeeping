#!/usr/bin/env bash

set -e

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

source $HOUSEKEEPING_TOOL_HOME/bin/common.sh
hadoop jar \
    $HOUSEKEEPING_TOOL_HOME/lib/housekeeping-vacuum-tool-* \
    com.hotels.housekeeping.tool.vacuum.VacuumTool \
    "$@"

exit
