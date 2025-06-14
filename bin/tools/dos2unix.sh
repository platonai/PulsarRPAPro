#!/bin/bash

# Convert files from DOS to Unix format
# Usage: dos2unix bin/tools/dos2unix.sh && bin/tools/dos2unix.sh

APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ "$APP_HOME" != "/" ]]; do
  if [[ -f "$APP_HOME/pom.xml" ]]; then
    break
  fi
  APP_HOME=$(dirname "$APP_HOME")
done

cd "$APP_HOME" || exit

dos2unix $@ "$APP_HOME"/mvnw

find "$APP_HOME"/bin -type f -name "*.sh" -print0 | xargs -0 dos2unix $@
dos2unix $@ "$APP_HOME"/VERSION

# find all bash files and add executable permission
find "$APP_HOME"/bin -type f -name "*.sh" -print0 | xargs -0 chmod +x
