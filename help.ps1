# Find the first parent directory containing the VERSION file
$scriptPath = $MyInvocation.MyCommand.Path
$AppHome = (Get-Item $scriptPath).Directory

while ($AppHome -ne $null -and !(Test-Path (Join-Path $AppHome.FullName "VERSION"))) {
    $AppHome = $AppHome.Parent
}

if ($AppHome -eq $null) {
    Write-Error "Could not find project root with VERSION file."
    exit 1
}

Set-Location $AppHome.FullName

# Check for Java
if (!(Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "Java is not installed or not in PATH."
    exit 1
}

# Locate JAR
$jarDir = Join-Path $AppHome.FullName "exotic-standalone/target"
$jarFiles = Get-ChildItem -Path $jarDir -Filter "PulsarRPAPro.jar" -Recurse -ErrorAction SilentlyContinue

if (-not $jarFiles) {
    # Build if no JAR found
    & (Join-Path $AppHome.FullName "mvnw") -DskipTests=true
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build failed."
        exit 1
    }
    $jarFiles = Get-ChildItem -Path $jarDir -Filter "PulsarRPAPro.jar" -Recurse
}

# Pick latest JAR
$latestJar = $jarFiles | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$jarPath = $latestJar.FullName

# Run application
Write-Output "Running application from $jarPath"
java -jar "$jarPath"
