# Tong GPT Windows Codex companion starter
# Run locally on YOUR Windows computer only, in PowerShell.
# Requires official OpenAI Codex CLI, Tailscale, and a signed-in Codex account.
# No credentials are bundled or uploaded to GitHub.
[CmdletBinding()]
param(
    [int]$CodexPort = 4500,
    [int]$TailnetHttpsPort = 8443,
    [switch]$RegenerateToken
)

$ErrorActionPreference = "Stop"
if (-not (Get-Command codex -ErrorAction SilentlyContinue)) {
    throw "Codex CLI not found. Install the official Codex CLI and sign in first."
}
if (-not (Get-Command tailscale -ErrorAction SilentlyContinue)) {
    throw "Tailscale not found. Install Tailscale and sign into the same tailnet on both devices."
}

$secretDir = Join-Path $env:USERPROFILE ".codex"
if (-not (Test-Path $secretDir)) {
    New-Item -ItemType Directory -Force -Path $secretDir | Out-Null
}
$tokenFile = Join-Path $secretDir "tong-gpt-remote.token"

if ($RegenerateToken -or -not (Test-Path $tokenFile)) {
    $random = [byte[]]::new(48)
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($random) } finally { $generator.Dispose() }
    $secret = [Convert]::ToBase64String($random).TrimEnd('=').Replace('+', '-').Replace('/', '_')
    [System.IO.File]::WriteAllText($tokenFile, $secret, [System.Text.Encoding]::ASCII)
    Write-Host "Created a new private capability token under your own Windows profile."
}
$secret = [System.IO.File]::ReadAllText($tokenFile).Trim()
if ($secret.Length -lt 32) { throw "Token file invalid; regenerate the token." }

Write-Host ""
Write-Host "Setting up a private Tailscale HTTPS reverse proxy; NOT Tailscale Funnel."
& tailscale serve --bg --https=$TailnetHttpsPort "http://127.0.0.1:$CodexPort"
if ($LASTEXITCODE -ne 0) {
    throw "Tailscale Serve failed. Enable tailnet HTTPS and check tailscale serve --help."
}

Write-Host ""
Write-Host "Find your device's tailnet hostname with: tailscale status"
Write-Host "Tong GPT WSS endpoint: wss://YOUR-WINDOWS-HOST.YOUR-TAILNET.ts.net:$TailnetHttpsPort/"
Write-Host "Windows Codex pairing token (keep private; never paste into issues, chat, or GitHub):"
Write-Host $secret
Write-Host ""
Write-Host "Codex stays on your Windows PC. Press Ctrl+C here to stop this foreground process."
Write-Host "For future unattended runs, configure a properly secured Windows service only after testing."
Write-Host ""
& codex app-server --listen "ws://127.0.0.1:$CodexPort" --ws-auth capability-token --ws-token-file $tokenFile
