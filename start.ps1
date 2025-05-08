# Find the first parent directory containing the VERSION file
$scriptPath = $MyInvocation.MyCommand.Path
$AppHome = (Get-Item $scriptPath).Directory

while ($AppHome -ne $null -and -not (Test-Path (Join-Path $AppHome.FullName "VERSION"))) {
  $AppHome = $AppHome.Parent
}

if ($AppHome -eq $null) {
  Write-Error "Could not find project root with VERSION file."
  exit 1
}

Set-Location $AppHome.FullName

# Locate JAR
$jarDir = Join-Path $AppHome.FullName "exotic-standalone/target"
$jarFiles = Get-ChildItem -Path $jarDir -Filter "PulsarRPAPro.jar" -Recurse -ErrorAction SilentlyContinue

if (-not $jarFiles) {
  Write-Output "No JAR found, building project..."
  & (Join-Path $AppHome.FullName "mvnw") -DskipTests=true
  if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed."
    exit 1
  }
  $jarFiles = Get-ChildItem -Path $jarDir -Filter "PulsarRPAPro.jar" -Recurse
}

# Pick latest JAR
$latestJar = $jarFiles | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$JAR = Resolve-Path $latestJar.FullName

# Handle command-line arguments
$URL = $args[0]
$otherArgs = $args[1..($args.Length - 1)]

Write-Output "Starting application: java $JVM_OPTS -jar $JAR serve $otherArgs"

# Run the application
try {
  java $JVM_OPTS -jar "$JAR" serve $otherArgs
} catch {
  Write-Error "Failed to execute the Java application: $_"
  exit 1
}
