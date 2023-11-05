$FILES=(Get-ChildItem -Path "exotic-standalone/target/" -Filter "exotic-standalone*.jar" -Recurse)
$FILE_COUNT = ($FILES | Measure-Object).Count

if ($FILE_COUNT -eq 0) {
    mvn -DskipTests=true
}

$JAR=(Resolve-Path $FILES[0])

java -jar "$JAR"
