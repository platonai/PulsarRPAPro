function printUsage {
  Write-Host "Usage: build.ps1 [-clean|-test]"
  exit 1
}

if ($args.Length -gt 0) {
  printUsage
}

$SKIP_TEST =$true
$CLEAN =$false

while ($args.Length -gt 0) {
  switch ($args[0]) {
    "-clean" {
      $CLEAN =$true
      $args =$args[1..($args.Length-1)] # past argument
    }
    "-skipTest" {
      $SKIP_TEST =$true
      $args =$args[1..($args.Length-1)] # past argument
    }
    { $_ -in "-h", "-help", "--help" } {
      printUsage
    }
    { $_ -in "-*", "--*" } {
      printUsage
    }
    default {
      printUsage
    }
  }
}

# Find the first parent directory containing the VERSION file
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Maven command and options
$MvnCmd = Join-Path $AppHome '.\mvnw.cmd'

if ($CLEAN) {
  & $MvnCmd clean
  if ($LastExitCode -ne 0) {
    exit $LastExitCode
  }
}

if ($SKIP_TEST) {
  & $MvnCmd -Pall-modules -DskipTests
} else {
  & $MvnCmd -Pall-modules
}

$exitCode =$LastExitCode
if ($exitCode -eq 0) {
  Write-Host "Build successfully"
} else {
  exit $exitCode
}
