#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)

if [ $# -lt 1 ]; then
  echo "usage: deploy-to-server.sh HOST [USER]"
  exit 1
fi

HOST=$1
shift

REMOTE_USER=$USER
if [ $# -ge 1 ]; then
  REMOTE_USER=$1
  shift
fi

# script config
VERSION_FILE=$(find . -name "VERSION")
VERSION_FILE=$(realpath "$VERSION_FILE")
APP_HOME=$(dirname "$VERSION_FILE")

SOURCE=$(find "$APP_HOME/exotic-server/target" -name "exotic-server*.jar")
SOURCE=$(realpath "$SOURCE")
VERSION=$(head -n 1 "$VERSION_FILE")
LOGBACK_CONFIG_FILE=$(find "$APP_HOME/exotic-standalone/src/main/resources" -name "logback-prod.xml")

REMOTE_WORKING_DIR="exotic/$VERSION"
ssh "$REMOTE_USER@$HOST" mkdir -p "$REMOTE_WORKING_DIR"
DESTINATION="$REMOTE_USER@$HOST:$REMOTE_WORKING_DIR"
mkdir -p "$HOME"/.pulsar/seeds

if [ -e "$SOURCE" ]; then
  echo "rsync --update -raz --progress $SOURCE $DESTINATION/"
  rsync --update -raz --progress "$SOURCE" "$DESTINATION/"
  rsync --update -raz --progress "$VERSION_FILE" "$DESTINATION/"
  rsync --update -raz --progress "$LOGBACK_CONFIG_FILE" "$DESTINATION/"
  rsync --update -raz --progress "$APP_HOME/bin/" "$DESTINATION/bin/"
  # rsync --update -raz --progress "$HOME/.pulsar/browser-easyrtc.zip" "$HOST:~/.pulsar/"
  # rsync --update -raz --progress "$HOME/.pulsar/seeds" "$HOST:~/.pulsar/"
else
  echo "$SOURCE does not exist"
  exit 1
fi

# ssh "$REMOTE_USER@$HOST" "$REMOTE_WORKING_DIR"/bin/deploy/correct-permissions.sh
