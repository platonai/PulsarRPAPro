FILE_COUNT=$(find . -wholename "exotic-standalone*.jar" | wc -l)

if (( FILE_COUNT == 0 )); then
  mvn -DskipTests=true
fi

cd exotic-standalone/target/ || exit
java -jar exotic-standalone*.jar serve
