# PowerShell脚本
$FILE_COUNT = (Get-ChildItem -Recurse -Filter "exotic-standalone*.jar").Count

if ($FILE_COUNT -eq 0) {
  mvn -DskipTests=true
}

# 确保目标目录存在，然后切换到该目录
$targetDir = "exotic-standalone\target\"
if (Test-Path $targetDir) {
  Set-Location -Path $targetDir -ErrorAction Stop
  # 获取匹配的jar文件
  $jarFile = Get-ChildItem -Filter "exotic-standalone*.jar" | Select-Object -First 1
  if ($jarFile) {
    # 使用完整文件名调用java -jar
    java -jar $jarFile.FullName serve
  } else {
    Write-Error "No exotic-standalone*.jar file found in $targetDir"
  }
} else {
  Write-Error "Directory $targetDir does not exist."
}

# 返回到原始目录
Pop-Location
