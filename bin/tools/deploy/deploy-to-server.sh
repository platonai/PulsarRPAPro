#bin

bin=$(dirname "$0")
bin=$(cd "$bin">/dev/null || exit; pwd)

if [ $# -lt 1 ]; then
  echo "usage: sync-server.sh HOST"
  exit 1
fi

HOST=$1
shift

# script config
VERSION_FILE=$(find . -name "VERSION")
VERSION_FILE=$(realpath "$VERSION_FILE")
APP_HOME=$(dirname "$VERSION_FILE")

SOURCE=$(find . -name "exotic-server*.jar")
SOURCE=$(realpath "$SOURCE")
VERSION=$(head -n 1 "$VERSION_FILE")
DESTINATION="$HOST:~/exotic-$VERSION"

if [ -e "$SOURCE" ]; then
  rsync --update -raz --progress "$SOURCE" "$DESTINATION/"
  rsync --update -raz --progress "$VERSION_FILE" "$DESTINATION/"
  rsync --update -raz --progress "$APP_HOME/bin/server/*" "$DESTINATION/bin/"
else
  echo "$SOURCE does not exist"
  exit 1
fi
