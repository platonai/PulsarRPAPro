# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome=$AppHome.Parent
}
cd $AppHome

$FILES=(Get-ChildItem -Path "$AppHome/exotic-standalone/target/" -Filter "PulsarRPAPro.jar" -Recurse)
$FILE_COUNT = ($FILES | Measure-Object).Count

if ($FILE_COUNT -eq 0) {
    mvn -DskipTests=true
}

$JAR=(Resolve-Path $FILES[0])

java -jar "$JAR"
