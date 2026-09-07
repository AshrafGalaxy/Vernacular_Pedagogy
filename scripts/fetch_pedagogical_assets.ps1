param(
    [string]$ManifestPath = "assets/images/asset_manifest.json"
)

$ErrorActionPreference = "Stop"
$manifest = Get-Content -Raw -LiteralPath $ManifestPath | ConvertFrom-Json
$rawDirectory = Join-Path (Split-Path $ManifestPath) "raw_sources"
New-Item -ItemType Directory -Force -Path $rawDirectory | Out-Null

foreach ($asset in $manifest.assets) {
    $destination = Join-Path $rawDirectory $asset.source_file
    if (Test-Path -LiteralPath $destination) {
        Write-Host "[SKIP] $($asset.source_file) already exists"
        continue
    }

    Write-Host "[FETCH] $($asset.motif_key)"
    Invoke-WebRequest -Uri $asset.source_url -OutFile $destination -MaximumRedirection 5
    if ((Get-Item -LiteralPath $destination).Length -eq 0) {
        throw "Downloaded source is empty: $destination"
    }
}
