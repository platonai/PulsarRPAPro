#!/bin/bash

if (( $# == 0 )); then
  echo "Usage: harvest.sh <URL>"
  exit 0
fi

FILE_COUNT=$(find "exotic-standalone/target/" -wholename "exotic-standalone*.jar" | wc -l)

if (( FILE_COUNT == 0 )); then
  mvn -DskipTests=true
fi

cd exotic-standalone/target/ || exit

URL=$1
shift

java -jar exotic-standalone*.jar harvest "$URL" -diagnose -vj
