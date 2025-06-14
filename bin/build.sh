#!/bin/bash

function printUsage {
  echo "Usage: build.sh [-clean|-test]"
  exit 1
}

SKIP_TEST=true
CLEAN=false

while [[ $# -gt 0 ]]; do
  case $1 in
    -clean)
      CLEAN=true
      shift # past argument
      ;;
    -skipTest)
      SKIP_TEST=true
      shift # past argument
      ;;
    -h|-help|--help)
      printUsage
      ;;
    -*)
      printUsage
      ;;
    *)
      printUsage
      ;;
  esac
done

# 查找包含 VERSION 文件的第一个父目录
APP_HOME=$(dirname "$(readlink -f "$0")")
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
  APP_HOME=$(dirname "$APP_HOME")
done
cd "$APP_HOME" || exit 1

# Maven 命令和选项
MVN_CMD="$APP_HOME/mvnw"
chmod +x "$MVN_CMD"

if [[ "$CLEAN" == true ]]; then
  $MVN_CMD clean
  if [[ $? -ne 0 ]]; then
    exit $?
  fi
fi

if [[ "$SKIP_TEST" == true ]]; then
  $MVN_CMD -Pall-modules -DskipTests
else
  $MVN_CMD -Pall-modules
fi

EXIT_CODE=$?
if [[ $EXIT_CODE -eq 0 ]]; then
  echo "Build successfully"
else
  exit $EXIT_CODE
fi