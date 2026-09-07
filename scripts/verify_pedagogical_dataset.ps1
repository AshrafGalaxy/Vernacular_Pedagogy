$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$fontPath = "assets/fonts/NotoSansOlChiki-Regular.ttf"
$fontCollection = New-Object System.Drawing.Text.PrivateFontCollection
$fontCollection.AddFontFile((Resolve-Path $fontPath))
if ($fontCollection.Families.Name -notcontains "Noto Sans Ol Chiki") { throw "Ol Chiki font failed to load." }

$flashcards = Get-Content -Raw -Encoding utf8 "assets/datasets/flashcard_seed_dataset.json" | ConvertFrom-Json
$worksheets = Get-Content -Raw -Encoding utf8 "assets/datasets/worksheet_template_seed_dataset.json" | ConvertFrom-Json
$seenCards = @{}
$olChikiRange = "[$([char]0x1C50)-$([char]0x1C7F)]"

foreach ($card in $flashcards) {
    if ($seenCards.ContainsKey($card.card_id)) { throw "Duplicate flashcard id: $($card.card_id)" }
    $seenCards[$card.card_id] = $true
    if ($card.grade_level -notin 1, 2, 3) { throw "Invalid grade: $($card.card_id)" }
    if ($card.lexical_payload.target_olchiki -notmatch $olChikiRange) { throw "Missing Ol Chiki: $($card.card_id)" }
    $path = Join-Path "assets" $card.asset_references.image_asset_path
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing card motif: $path" }
}

foreach ($worksheet in $worksheets) {
    if ($worksheet.target_grade -notin 1, 2, 3) { throw "Invalid worksheet grade: $($worksheet.template_id)" }
    foreach ($exercise in $worksheet.exercises) {
        foreach ($slot in $exercise.item_slots) {
            $path = "assets/images/motifs/motif_$($slot.motif_key).png"
            if (-not (Test-Path -LiteralPath $path)) { throw "Missing worksheet motif: $path" }
            if ($slot.target_count -lt 1 -or $slot.target_count -gt 20) { throw "Unsupported item count: $($slot.slot_id)" }
        }
    }
}

Write-Host "[PASS] Noto Sans Ol Chiki loaded"
Write-Host "[PASS] $($flashcards.Count) flashcard records"
Write-Host "[PASS] $($worksheets.Count) worksheet templates"
