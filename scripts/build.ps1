$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$mainOut = Join-Path $root "out\main"

New-Item -ItemType Directory -Force $mainOut | Out-Null
$sources = Get-ChildItem -Path (Join-Path $root "src\main\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }

javac -encoding UTF-8 -source 8 -target 8 -d $mainOut $sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
Write-Host "Build completed: $mainOut"
