# run-bot.ps1
# Levanta SOLO el bot (sin Docker), apuntando al backend en la nube (Render).
# Carga los secretos desde .env y arranca con mvnw. No necesita backend ni Mongo local.
# BACKEND_URL / TMA_URL toman su default de prod (application.properties); overridealos
# en el .env si queres apuntar a otro lado.

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$envFile = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path $envFile)) {
    Write-Host "Falta el archivo .env (copia .env.example y completa los secretos)." -ForegroundColor Red
    exit 1
}

Write-Host "==> Cargando variables de .env..." -ForegroundColor Cyan
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        Set-Item -Path "env:$name" -Value $value
    }
}

Write-Host "==> Bot -> backend: $(if ($env:BACKEND_URL) { $env:BACKEND_URL } else { 'default de application.properties (Render)' })" -ForegroundColor Cyan
if (-not $env:WEBHOOK_URL) {
    Write-Host "==> OJO: WEBHOOK_URL vacio -> el bot levanta pero NO recibe mensajes. Expone el :8081 con un tunel (ngrok/cloudflared) y pone su URL https en WEBHOOK_URL (ver README)." -ForegroundColor Yellow
}
Write-Host "==> Arrancando el bot en :8081 (webhook, Ctrl+C para cortar)..." -ForegroundColor Green
& "$PSScriptRoot\mvnw.cmd" spring-boot:run
