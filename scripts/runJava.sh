#!/bin/bash

function runJava {
  
  # Figure out where this script lives in 
  SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

  # Prepare main variables used to do the java call
  MAIN=$1
  shift 1
  ARGS=$@
  REP_ROOT="$(dirname "$DIR")"
  # Simple script to run the java program from the eclipse-generated .classpath file (used for development)

  #CLASSPATH=${REP_ROOT}/bin/:`cat ${REP_ROOT}/.classpath | grep jar | sed 's/.* path..//' | sed 's/\".*//' | tr "\n" ":"`
  #CLASSPATH=${CLASSPATH}:`cat ${REP_ROOT}/.classpath | grep combineaccessrules | sed "s#.* path..#${REP_ROOT}/..#" | sed 's#\".*#/bin/#' | tr "\n" ":"`

  # Use the output from gradle installApp
  CLASSPATH=${REP_ROOT}/build/install/westrun/lib/\*

  java -Xmx2g -cp "${CLASSPATH}" ${MAIN} ${ARGS}

}