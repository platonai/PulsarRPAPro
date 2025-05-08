# PowerShell脚本
# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome=$AppHome.Parent
}
cd $AppHome

$FILES=(Get-ChildItem -Path "$AppHome/exotic-standalone/target/" -Filter "PulsarRPAPro.jar" -Recurse)
$FILE_COUNT = ($FILES | Measure-Object).Count

if ($FILE_COUNT -eq 0) {
  &"$AppHome/mvnw" -DskipTests=true
}

$JAR=(Resolve-Path $FILES[0])

$URL = $args[0]
$args = $args[1..($args.Length - 1)]

Write-Output "java $JVM_OPTS -jar $JAR serve $args"

try {
  java $JVM_OPTS -jar "$JAR" serve "$args"
} catch {
  Write-Error "Failed to execute the Java application: $_"
}
