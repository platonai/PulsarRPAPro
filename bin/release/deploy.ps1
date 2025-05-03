function Remove-LocalPlatonJars {
  Write-Host "Ready to remove local platon jars (~/.m2/repository/ai/platon/*)"
  $reply = Read-Host -Prompt "Are you sure to continue? [Y/n]"
  if ($reply -match "^[Yy]$") {
    Rename-Item -Path "$env:USERPROFILE\.m2\repository\ai\platon" -NewName "platon_bak"
  } else {
    Write-Host "Local platon jars are kept."
  }
}

function Remove-LocalStorage {
  Write-Host "Ready to remove local storage"
  $reply = Read-Host -Prompt "Are you sure to continue? [Y/n]"
  if ($reply -match "^[Yy]$") {
    Remove-Item -Path "$env:USERPROFILE\.pulsar\data\store\*" -Recurse -Force
  } else {
    Write-Host "Local platon jars are kept."
  }
}

$bin = Split-Path -Parent $MyInvocation.MyCommand.Definition
$bin = Resolve-Path (Join-Path $bin "..")
$AppHome = Resolve-Path (Join-Path $bin "..")

Set-Location -Path $AppHome

Write-Host "Deploy the project ..."
Write-Host "Changing version ..."
Write-Host ""

$User = "vincent"
$TargetHost = "platonai.cn"

$SNAPSHOT_VERSION = Get-Content -Path "$AppHome\VERSION" -TotalCount 1
$VERSION = $SNAPSHOT_VERSION -replace "-SNAPSHOT", ""
Set-Content -Path "$AppHome\VERSION" -Value $VERSION

Get-ChildItem -Path $AppHome -Filter pom.xml -Recurse | ForEach-Object {
  (Get-Content -Path $_.FullName) -replace $SNAPSHOT_VERSION, $VERSION | Set-Content -Path $_.FullName
}

$SNIPPET = Select-String -Path "$AppHome\pom.xml" -Pattern "-SNAPSHOT"
if ($SNIPPET) {
  Write-Host "Found SNAPSHOT artifacts in your pom.xml:"
  Write-Host ">>>>>>>>"
  Write-Host $SNIPPET
  Write-Host "<<<<<<<<"
  Write-Host "Please upgrade to release versions of the artifacts"
  exit 0
}

# Maven command and options
$MvnCmd = Join-Path $AppHome '.\mvnw.cmd'

# Use the product version of logback
Copy-Item -Path "$AppHome\exotic-standalone\src\main\resources\logback-prod.xml" -Destination "$AppHome\exotic-standalone\src\main\resources\logback.xml" -Force

Remove-LocalPlatonJars

Write-Host "Cleaning Maven build..."
# Start-Process -FilePath "mvn" -ArgumentList "clean" -NoNewWindow -Wait
# Start-Process -FilePath "mvn" -ArgumentList "" -NoNewWindow -Wait
& $MvnCmd clean install

if ($LASTEXITCODE -ne 0) {
  Write-Host "Build failed!"
  exit 1
}

Set-Location -Path "$AppHome\exotic-services"
# Start-Process -FilePath "mvn" -ArgumentList "-PREST-war war:war" -NoNewWindow -Wait
& $MvnCmd -PREST-war war:war

if ($LASTEXITCODE -ne 0) {
  Write-Host "Build war failed!"
  exit 1
}

if ($LASTEXITCODE -ne 0) {
  Write-Host "WAR build failed!"
  exit 1
}

Set-Location -Path $AppHome

$REMOTE_BASE_DIR = "/home/$env:USERNAME/platonai.cn/repo/ai/platon/exotic"

# Create remote directory
ssh $TargetHost "mkdir -p $REMOTE_BASE_DIR"

# Copy files to remote host using scp
scp "$AppHome\exotic-services\target\exotic.war" "$User@${TargetHost}:$REMOTE_BASE_DIR/"
scp "$AppHome\exotic-standalone\target\exotic-standalone-$VERSION.jar" "$User@${TargetHost}:$REMOTE_BASE_DIR/"

if ($LASTEXITCODE -ne 0) {
  Write-Host "Failed to copy files to remote destination!"
  exit 1
}

Write-Host "List directory before creating symbolic link: "
ssh $TargetHost "ls -l $REMOTE_BASE_DIR"

ssh $TargetHost "unlink $REMOTE_BASE_DIR/PulsarRPAPro.jar"
ssh $TargetHost "ln -s $REMOTE_BASE_DIR/exotic-standalone-$VERSION.jar $REMOTE_BASE_DIR/PulsarRPAPro.jar"

Write-Host "List directory after creating symbolic link: "
ssh $TargetHost "ls -l $REMOTE_BASE_DIR"

Write-Host "Deployment completed successfully!"
