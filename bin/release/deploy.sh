#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
APP_HOME=$(cd "$bin"/..>/dev/null || exit; pwd)

cd "$APP_HOME" || exit;

echo "Deploy the project ..."
echo "Changing version ..."

HOST=master

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=${SNAPSHOT_VERSION//"-SNAPSHOT"/""}
echo "$VERSION" > "$APP_HOME"/VERSION
find "$APP_HOME" -name 'pom.xml' -exec sed -i "s/$SNAPSHOT_VERSION/$VERSION/" {} \;

mvn clean
mvn

REMOTE_BASE_DIR=~/platonic.fun/repo/ai/platon/exotic
ssh $HOST mkdir -p $REMOTE_BASE_DIR

scp -r "$APP_HOME"/exotic-standalone/target/exotic-standalone-"$VERSION".jar "$HOST:$REMOTE_BASE_DIR/"

echo "List directory before creating symbolic link: "
ssh $HOST ls -l $REMOTE_BASE_DIR
ssh $HOST unlink $REMOTE_BASE_DIR/exotic-standalone.jar
ssh $HOST ln -s $REMOTE_BASE_DIR/exotic-standalone-"$VERSION".jar $REMOTE_BASE_DIR/exotic-standalone.jar
echo "List directory after creating symbolic link: "
ssh $HOST ls -l $REMOTE_BASE_DIR
