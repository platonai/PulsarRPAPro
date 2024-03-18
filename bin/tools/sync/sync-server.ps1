# bin
$bin = Split-Path $MyInvocation.MyCommand.Path -Parent

if ($args.Count -lt 1) {
  Write-Host "usage: sync-server.ps1 HOST"
  exit 1
}

$HOST = $args[0]
$args = $args[1..($args.Count-1)]

# script config
$VERSION_FILE = Get-ChildItem -Path $bin -Filter "VERSION" -Recurse | Select-Object -First 1
$VERSION_FILE = $VERSION_FILE.FullName
$APP_HOME = (Get-Item $VERSION_FILE).Directory.FullName

$SOURCE = Get-ChildItem -Path $bin -Filter "exotic-server*.jar" -Recurse | Select-Object -First 1
$SOURCE = $SOURCE.FullName
$VERSION = Get-Content -Path $VERSION_FILE -TotalCount 1
$DESTINATION = "$HOST:~/exotic-$VERSION"

if (Test-Path $SOURCE) {
  robocopy $SOURCE "$DESTINATION" /E
  robocopy "$APP_HOME\bin\server\" "$DESTINATION" /E
}
else {
  Write-Host "$SOURCE does not exist"
  exit 1
}
