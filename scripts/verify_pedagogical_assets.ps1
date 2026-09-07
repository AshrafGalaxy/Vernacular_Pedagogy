param(
    [string]$ManifestPath = "assets/images/asset_manifest.json"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
$manifest = Get-Content -Raw -LiteralPath $ManifestPath | ConvertFrom-Json
$imageRoot = Split-Path $ManifestPath
$motifDirectory = Join-Path $imageRoot "motifs"

foreach ($asset in $manifest.assets) {
    $path = Join-Path $motifDirectory $asset.output_file
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing motif: $path" }
    $bitmap = [System.Drawing.Bitmap]::FromFile($path)
    try {
        if ($bitmap.Width -ne 400 -or $bitmap.Height -ne 400) { throw "Invalid dimensions: $path" }
        if ((Get-Item -LiteralPath $path).Length -gt 45KB) { throw "Motif exceeds 45 KB: $path" }
    } finally { $bitmap.Dispose() }
    Write-Host "[PASS] $($asset.output_file)"
}
Write-Host "[PASS] $($manifest.assets.Count) worksheet/flashcard motifs verified"
