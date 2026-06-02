# stop-all.ps1
# Apaga el stack completo: primero el bot, despues el backend + mongo.
# Usa "docker compose down" (sin -v): NO borra volumenes (Mongo y sesiones se conservan).
# Para un reset limpio de la base, correr a mano en tp1c2026/backend:  docker compose down -v

$botDir = $PSScriptRoot
$backendDir = Resolve-Path (Join-Path $PSScriptRoot '..\tp1c2026\backend')

Write-Host "==> Bajando el bot ($botDir)..." -ForegroundColor Cyan
Push-Location $botDir
docker compose down
Pop-Location

Write-Host "==> Bajando backend + mongo ($backendDir)..." -ForegroundColor Cyan
Push-Location $backendDir
docker compose down
Pop-Location

Write-Host "==> Listo. Contenedores que siguen corriendo:" -ForegroundColor Green
docker ps --format "table {{.Names}}`t{{.Status}}"
