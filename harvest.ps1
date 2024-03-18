if ($args.Length -eq 0) {
    Write-Host "Usage: harvest.ps1 <URL>"
    exit 0
}

$FILES=(Get-ChildItem -Path "exotic-standalone/target/" -Filter "exotic-standalone*.jar" -Recurse)
$FILE_COUNT = ($FILES | Measure-Object).Count

if ($FILE_COUNT -eq 0) {
    mvn -DskipTests=true
}

$JAR=(Resolve-Path $FILES[0])

$URL = $args[0]
$args = $args[1..($args.Length - 1)]

java -jar "$JAR" harvest "$URL" -diagnose -vj $args
