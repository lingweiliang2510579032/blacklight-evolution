# Blacklight Evolution - D 盘编译与测试脚本
# 用法: powershell -ExecutionPolicy Bypass -File build_and_test.ps1
$ErrorActionPreference = "Stop"
$root = "D:\Blacklight-Evolution-0.2.0-alpha.2\blacklight-evolution-neoforge"
Set-Location $root

Write-Host "=== Blacklight Evolution build & test ===" -ForegroundColor Cyan
Write-Host "Working dir: $root"

# 先尝试正常构建(依赖缓存已就绪,cloudsmith/modrinth/central 可达)
& ".\gradlew.bat" clean build --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) {
    Write-Host "Normal build failed (exit $LASTEXITCODE), retrying with --offline ..." -ForegroundColor Yellow
    & ".\gradlew.bat" build --offline --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) {
        Write-Host "BUILD FAILED (offline also failed, exit $LASTEXITCODE)" -ForegroundColor Red
        exit 1
    }
}

Write-Host "=== Build OK. Test results ===" -ForegroundColor Green
$results = Get-ChildItem "$root\build\test-results\test" -Filter "TEST-*.xml" -ErrorAction SilentlyContinue
$total = 0; $fail = 0; $err = 0; $skip = 0
foreach ($f in $results) {
    [xml]$x = Get-Content $f.FullName
    $total += [int]$x.testsuite.tests
    $fail += [int]$x.testsuite.failures
    $err += [int]$x.testsuite.errors
    $skip += [int]$x.testsuite.skipped
}
Write-Host ("Tests: {0} total, {1} failures, {2} errors, {3} skipped" -f $total, $fail, $err, $skip)
if ($fail -gt 0 -or $err -gt 0) { exit 1 }

Write-Host "=== Artifact ===" -ForegroundColor Green
Get-ChildItem "$root\build\libs" | ForEach-Object { Write-Host ("{0}  ({1:N1} KB)" -f $_.Name, ($_.Length / 1KB)) }
Write-Host "DONE" -ForegroundColor Green
