#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
APP_HOME=$(cd "$bin"/..>/dev/null || exit; pwd)

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=$(sed 's/\(.*\)-.*/\1/' <<< $SNAPSHOT_VERSION)
PREFIX=$(echo "$VERSION" | cut -d'.' -f1,2)
MINOR_VERSION=$(echo "$VERSION" | cut -d'.' -f3)
MINOR_VERSION=$(("$MINOR_VERSION" + 1))

NEXT_VERSION="$PREFIX.$MINOR_VERSION-SNAPSHOT"

echo "New version: $NEXT_VERSION"
echo "$NEXT_VERSION" > "$APP_HOME"/VERSION
find "$APP_HOME" -name 'pom.xml' -exec sed -i "s/$SNAPSHOT_VERSION/$NEXT_VERSION/" {} \;

COMMENT=$(sed 's/\(.*\)-.*/\1/' <<< $NEXT_VERSION)

echo "Ready to commit with comment: $COMMENT"
read -p "Are you sure to continue?" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then
  git add .
  git commit -m "$COMMENT"
  git push
fi
