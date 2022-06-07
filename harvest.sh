FILE_COUNT=$(find "exotic-standalone/target/" -wholename "exotic-standalone*.jar" | wc -l)

if (( FILE_COUNT == 0 )); then
  mvn -DskipTests=true
fi

cd exotic-standalone/target/ || exit
java -jar exotic-standalone*.jar harvest https://www.amazon.com/Best-Sellers-Automotive/zgbs/automotive/ -diagnose -vj
