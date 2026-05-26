$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$mainOut = Join-Path $root "out\main"
$testOut = Join-Path $root "out\test"

& (Join-Path $PSScriptRoot "build.ps1")
New-Item -ItemType Directory -Force $testOut | Out-Null
$tests = Get-ChildItem -Path (Join-Path $root "src\test\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }

javac -encoding UTF-8 -source 8 -target 8 -cp $mainOut -d $testOut $tests
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
java -cp "$mainOut;$testOut" com.health.modulea.ModuleATest
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
