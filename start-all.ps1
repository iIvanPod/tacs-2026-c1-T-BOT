# start-all.ps1
# Levanta el stack completo: backend + mongo (compose de tp1c2026/backend) y despues el bot.
# Cada compose corre en su propia carpeta, asi cada uno lee SU propio .env
# (los secretos del bot -TELEGRAM_*, GEMINI_API_KEY- quedan en el .env del bot, no en el del backend).

$ErrorActionPreference = 'Stop'

$botDir = $PSScriptRoot
$backendDir = Resolve-Path (Join-Path $PSScriptRoot '..\tp1c2026\backend')

Write-Host "==> Levantando backend + mongo ($backendDir)..." -ForegroundColor Cyan
Push-Location $backendDir
docker compose up -d
$backendExit = $LASTEXITCODE
Pop-Location
if ($backendExit -ne 0) {
    Write-Host "El backend no levanto (exit $backendExit). Aborto sin levantar el bot." -ForegroundColor Red
    exit $backendExit
}

Write-Host "==> Levantando bot ($botDir)..." -ForegroundColor Cyan
Push-Location $botDir
docker compose up --build -d
$botExit = $LASTEXITCODE
Pop-Location
if ($botExit -ne 0) {
    Write-Host "El bot no levanto (exit $botExit)." -ForegroundColor Red
    exit $botExit
}

Write-Host "==> Listo. Contenedores corriendo:" -ForegroundColor Green
docker ps --format "table {{.Names}}`t{{.Status}}`t{{.Ports}}"
