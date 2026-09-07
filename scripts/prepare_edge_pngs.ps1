param(
    [string]$ManifestPath = "assets/images/asset_manifest.json"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$manifest = Get-Content -Raw -LiteralPath $ManifestPath | ConvertFrom-Json
$imageRoot = Split-Path $ManifestPath
$rawDirectory = Join-Path $imageRoot "raw_sources"
$outputDirectory = Join-Path $imageRoot "motifs"
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

foreach ($asset in $manifest.assets) {
    $sourcePath = Join-Path $rawDirectory $asset.source_file
    $outputPath = Join-Path $outputDirectory $asset.output_file
    if (-not (Test-Path -LiteralPath $sourcePath)) {
        throw "Missing source image: $sourcePath. Run fetch_pedagogical_assets.ps1 first."
    }

    $source = [System.Drawing.Bitmap]::FromFile($sourcePath)
    try {
        $side = [Math]::Max($source.Width, $source.Height)
        $square = New-Object System.Drawing.Bitmap $side, $side, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $squareGraphics = [System.Drawing.Graphics]::FromImage($square)
        try {
            $squareGraphics.Clear([System.Drawing.Color]::White)
            $x = [int](($side - $source.Width) / 2)
            $y = [int](($side - $source.Height) / 2)
            $squareGraphics.DrawImage($source, $x, $y, $source.Width, $source.Height)
        } finally {
            $squareGraphics.Dispose()
        }

        $resized = New-Object System.Drawing.Bitmap 400, 400, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $resizeGraphics = [System.Drawing.Graphics]::FromImage($resized)
        try {
            $resizeGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $resizeGraphics.DrawImage($square, 0, 0, 400, 400)
        } finally {
            $resizeGraphics.Dispose()
            $square.Dispose()
        }

        for ($y = 0; $y -lt 400; $y++) {
            for ($x = 0; $x -lt 400; $x++) {
                $pixel = $resized.GetPixel($x, $y)
                $luminance = (0.2126 * $pixel.R) + (0.7152 * $pixel.G) + (0.0722 * $pixel.B)
                if ($luminance -lt 150) {
                    $resized.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 0, 0, 0))
                } else {
                    $resized.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
                }
            }
        }
        $resized.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $resized.Dispose()
        Write-Host "[COMPILED] $($asset.output_file)"
    } finally {
        $source.Dispose()
    }
}
