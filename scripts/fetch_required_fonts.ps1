$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
$fontDirectory = "assets/fonts"
New-Item -ItemType Directory -Force -Path $fontDirectory | Out-Null

$fontUrl = "https://android.googlesource.com/platform/external/noto-fonts/+/a19a3af6f202af6785a4449fb807e406a548299a/notosansolchiki/NotoSansOlChiki-Regular.ttf?format=TEXT"
$fontPath = Join-Path $fontDirectory "NotoSansOlChiki-Regular.ttf"
$noticePath = Join-Path $fontDirectory "NotoSansOlChiki-LICENSE.txt"

if (-not (Test-Path -LiteralPath $fontPath)) {
    $encodedPath = Join-Path $fontDirectory "NotoSansOlChiki-Regular.ttf.base64"
    try {
        & curl.exe -L --fail --silent --show-error --output $encodedPath $fontUrl
        if ($LASTEXITCODE -ne 0) { throw "Official font download failed." }
        $encoded = [System.IO.File]::ReadAllText($encodedPath).Trim()
        [System.IO.File]::WriteAllBytes($fontPath, [Convert]::FromBase64String($encoded))
    } finally {
        if (Test-Path -LiteralPath $encodedPath) { Remove-Item -LiteralPath $encodedPath }
    }
}

@"
Noto Sans Ol Chiki Regular
Source: Android Open Source Project, noto-fonts
Source URL: $fontUrl
License: SIL Open Font License 1.1 (OFL-1.1)
"@ | Set-Content -LiteralPath $noticePath -Encoding utf8

if ((Get-Item -LiteralPath $fontPath).Length -lt 5KB) {
    throw "Downloaded font is unexpectedly small: $fontPath"
}
$fontCollection = New-Object System.Drawing.Text.PrivateFontCollection
$fontCollection.AddFontFile((Resolve-Path $fontPath))
if ($fontCollection.Families.Name -notcontains "Noto Sans Ol Chiki") {
    throw "Downloaded font does not expose the expected Noto Sans Ol Chiki family."
}
Write-Host "[PASS] $fontPath"
