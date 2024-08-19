# PowerShell脚本
# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome=$AppHome.Parent
}
cd $AppHome

# Check java version, make sure it is Java 11
$JAVA_VERSION = (java -version 2>&1 | Select-String "version" | Select-String "11\.")

if ($JAVA_VERSION -eq $null) {
  Write-Output "WARNING: Java 11 is required to run this program"
}

$FILES=(Get-ChildItem -Path "$AppHome/exotic-standalone/target/" -Filter "exotic-standalone*.jar" -Recurse)
$FILE_COUNT = ($FILES | Measure-Object).Count

if ($FILE_COUNT -eq 0) {
  mvn -DskipTests=true
}

$JAR=(Resolve-Path $FILES[0])

$URL = $args[0]
$args = $args[1..($args.Length - 1)]

# --add-opens java.base/java.time=ALL-UNNAMED to fix JEP 396: Strongly Encapsulate JDK Internals by Default,
# the problem appears when upgrading java from 11 to 17.
# $JVM_OPTS = "--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED"
# $JVM_OPTS = "--add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED"

Write-Output "java $JVM_OPTS -jar $JAR serve $args"

try {
  java $JVM_OPTS -jar "$JAR" serve "$args"
} catch {
  Write-Error "Failed to execute the Java application: $_"
}
