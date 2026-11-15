#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Start Up Shell Script
# ----------------------------------------------------------------------------

which mvn >/dev/null 2>&1
if [ $? -eq 0 ]; then
  exec mvn "$@"
fi

if [ -z "$JAVA_HOME" ] ; then
  echo "Error: JAVA_HOME is not set." >&2
  exit 1
fi

WRAPPER_JAR="$(dirname "$0")/.mvn/wrapper/maven-wrapper.jar"
if [ -r "$WRAPPER_JAR" ]; then
    exec "$JAVA_HOME/bin/java" -jar "$WRAPPER_JAR" "$@"
else
    echo "Error: Could not find maven-wrapper.jar and mvn is not in PATH." >&2
    exit 1
fi

