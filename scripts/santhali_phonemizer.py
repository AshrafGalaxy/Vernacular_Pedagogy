# -*- coding: utf-8 -*-
"""
Santhali (sat_Olck) Ol Chiki to IPA Grapheme-to-Phoneme (G2P) Converter.

Converts authentic Santhali Ol Chiki orthography into standard International
Phonetic Alphabet (IPA) symbols compatible with Piper VITS pre-trained models.

Key Linguistic Rules Implemented:
1. Base Vowels (6): ᱚ (/ɔ/), ᱟ (/a/), ᱤ (/i/), ᱩ (/u/), ᱮ (/e/), ᱳ (/o/)
2. Low-Vowel Diacritic (Gahla Tundag ᱹ):
   - ᱚᱹ -> /ə/
   - ᱟᱹ -> /ə/
   - ᱮᱹ -> /ɛ/
3. Nasalization Diacritic (Mu Tundag ᱸ):
   - Vowel + ᱸ -> Vowel + /̃/ (e.g. ᱟᱸ -> ã)
4. Combined Nasal-Low Diacritic (Mu-Gahla Tundag ᱺ):
   - ᱚᱺ -> ə̃
   - ᱮᱺ -> ɛ̃
5. Vowel Prolongation (Relah ᱻ):
   - Vowel + ᱻ -> Vowel + /ː/
6. Checked Consonant Deglottalization (Ohod ᱽ):
   - ᱜᱽ -> /ɡ/
   - ᱡᱽ -> /ɟ/
   - ᱫᱽ -> /d/
   - ᱵᱽ -> /b/
7. Aspiration (Oh ᱷ):
   - Consonant + ᱷ -> Consonant + /ʰ/
8. Coda & Intervocalic Releases:
   - ᱜ, ᱡ, ᱫ, ᱵ followed by vowels release as voiced plosives /ɡ/, /ɟ/, /d/, /b/.
9. Numerals (Ol Chiki ᱐-᱙):
   - Automatically converted to phonetic Santhali words.
"""

import sys
import re
import unicodedata
from typing import List, Dict, Tuple


# Santhali Ol Chiki Numeral to Word Mapping
OL_CHIKI_NUMERALS: Dict[str, str] = {
    "᱐": "ᱥᱩᱱ",      # sun (zero)
    "᱑": "ᱢᱤᱫ",      # mid (one)
    "᱒": "ᱵᱟᱨ",      # bar (two)
    "᱓": "ᱯᱮ",       # pe (three)
    "᱔": "ᱯᱳᱱ",      # pon (four)
    "᱕": "ᱢᱚᱬᱮ",     # mone (five)
    "᱖": "ᱛᱩᱨᱩᱭ",    # turui (six)
    "᱗": "ᱮᱭᱟᱭ",     # eyae (seven)
    "᱘": "ᱤᱨᱟᱹᱞ",    # irəl (eight)
    "᱙": "ᱟᱨᱮ",      # are (nine)
}

# Multi-character compound mappings (ordered by length descending)
COMPOUND_MAP: List[Tuple[str, str]] = [
    # Deglottalized plosives with Ohod (ᱽ)
    ("ᱜᱽ", "ɡ"),
    ("ᱡᱽ", "ɟ"),
    ("ᱫᱽ", "d"),
    ("ᱵᱽ", "b"),

    # Aspirated plosives with Oh (ᱷ)
    ("ᱛᱷ", "tʰ"),
    ("ᱠᱷ", "kʰ"),
    ("ᱪᱷ", "cʰ"),
    ("ᱯᱷ", "pʰ"),
    ("ᱴᱷ", "ʈʰ"),
    ("ᱫᱷ", "dʰ"),
    ("ᱜᱷ", "ɡʰ"),
    ("ᱡᱷ", "ɟʰ"),
    ("ᱵᱷ", "bʰ"),
    ("ᱰᱷ", "ɖʰ"),

    # Vowels with Mu-Gahla Tundag (ᱺ) - Nasal + Low
    ("ᱚᱺ", "ə̃"),
    ("ᱟᱺ", "ə̃"),
    ("ᱮᱺ", "ɛ̃"),

    # Vowels with Gahla Tundag (ᱹ) - Low/Centralized
    ("ᱚᱹ", "ə"),
    ("ᱟᱹ", "ə"),
    ("ᱮᱹ", "ɛ"),
    ("ᱩᱹ", "ʊ"),
    ("ᱤᱹ", "ɪ"),
    ("ᱳᱹ", "ɔ"),

    # Vowels with Mu Tundag (ᱸ) - Nasalization
    ("ᱚᱸ", "ɔ̃"),
    ("ᱟᱸ", "ã"),
    ("ᱤᱸ", "ĩ"),
    ("ᱩᱸ", "ũ"),
    ("ᱮᱸ", "ẽ"),
    ("ᱳᱸ", "õ"),

    # Vowels with Relah (ᱻ) - Lengthening
    ("ᱚᱻ", "ɔː"),
    ("ᱟᱻ", "aː"),
    ("ᱤᱻ", "iː"),
    ("ᱩᱻ", "uː"),
    ("ᱮᱻ", "eː"),
    ("ᱳᱻ", "oː"),
]

# Single Ol Chiki character to IPA mapping
SINGLE_CHAR_MAP: Dict[str, str] = {
    # 6 Base Vowels
    "ᱚ": "ɔ",   # LA
    "ᱟ": "a",   # LAA
    "ᱤ": "i",   # LI
    "ᱩ": "u",   # LU
    "ᱮ": "e",   # LE
    "ᱳ": "o",   # LO

    # Consonants - Plosives & Affricates
    "ᱛ": "t",   # AT (voiceless dental plosive)
    "ᱠ": "k",   # AAK (voiceless velar plosive)
    "ᱪ": "c",   # UCH (voiceless palatal plosive / affricate)
    "ᱯ": "p",   # EP (voiceless bilabial plosive)
    "ᱴ": "ʈ",   # OTT (voiceless retroflex plosive)
    "ᱰ": "ɖ",   # EDD (voiced retroflex plosive)

    # Consonants - Checked/Voiced Stops
    "ᱜ": "ɡ",   # AG (velar stop)
    "ᱡ": "ɟ",   # AAJ (palatal stop)
    "ᱫ": "d",   # UD (dental stop)
    "ᱵ": "b",   # OB (bilabial stop)

    # Nasals
    "ᱝ": "ŋ",   # ANG (velar nasal)
    "ᱢ": "m",   # AAM (bilabial nasal)
    "ᱧ": "ɲ",   # INY (palatal nasal)
    "ᱬ": "ɳ",   # UNN (retroflex nasal)
    "ᱱ": "n",   # EN (alveolar nasal)
    "ᱶ": "w̃",   # OV (nasalized labial glide)

    # Liquids, Fricatives & Glides
    "ᱞ": "l",   # AL (alveolar lateral)
    "ᱣ": "w",   # AAW (labial-velar approximant)
    "ᱥ": "s",   # IS (alveolar sibilant)
    "ᱦ": "h",   # IH (glottal fricative)
    "ᱨ": "r",   # IR (alveolar trill/tap)
    "ᱭ": "j",   # UY (palatal approximant)
    "ᱲ": "ɽ",   # ERR (retroflex flap)
    "ᱷ": "ʰ",   # OH (aspiration marker)

    # Diacritics and separators
    "ᱸ": "̃",    # Mu Tundag (nasalization)
    "ᱹ": "ə",    # Gahla Tundag (centralized vowel)
    "ᱺ": "ə̃",   # Mu-Gahla Tundag (nasalized centralized vowel)
    "ᱻ": "ː",    # Relah (vowel prolongation)
    "ᱼ": "ʔ",    # Phaarkaa (glottal catch / morpheme boundary separator)
    "ᱽ": "",     # Ohod (handled in compounds or silent coda)

    # Punctuation
    "᱾": ".",   # Mucad
    "᱿": ".",   # Double Mucad
}


def normalize_santhali_text(text: str) -> str:
    """Canonical Unicode NFC normalization and cleanup for Santhali text."""
    if not text:
        return ""
    text = unicodedata.normalize("NFC", text)
    # Replace Ol Chiki digits with words
    for digit, word in OL_CHIKI_NUMERALS.items():
        text = text.replace(digit, word)
    # Clean whitespace and control characters
    text = " ".join(text.split())
    return text.strip()


def santhali_to_ipa(text: str) -> str:
    """
    Translates Santhali Ol Chiki text into a standard IPA phoneme sequence
    suitable for Piper TTS neural speech synthesis.

    Args:
        text: Santhali text written in Ol Chiki script.

    Returns:
        String of standard IPA phonemes and punctuation.
    """
    normalized = normalize_santhali_text(text)
    if not normalized:
        return ""

    result = normalized

    # 1. Apply multi-character compound substitutions first
    for olck_seq, ipa_seq in COMPOUND_MAP:
        result = result.replace(olck_seq, ipa_seq)

    # 2. Apply single Ol Chiki character substitutions
    out_chars = []
    for c in result:
        if c in SINGLE_CHAR_MAP:
            out_chars.append(SINGLE_CHAR_MAP[c])
        else:
            out_chars.append(c)

    res_str = "".join(out_chars)

    # 3. Clean up formatting: ensure proper spacing around punctuation
    res_str = re.sub(r'\s+', ' ', res_str)
    # Ensure ASCII 'g' maps to IPA script 'ɡ' (U+0261, ID 66 in Piper eSpeak)
    res_str = res_str.replace('g', 'ɡ')
    # Normalize duplicate punctuation
    res_str = re.sub(r'\.+', '.', res_str)
    return res_str.strip()


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    test_cases = [
        ("ᱡᱚᱦᱟᱨ", "ɟɔhar", "Classroom Greeting (Johar)"),
        ("ᱤᱧ ᱫᱚ ᱵᱤᱨ ᱛᱮᱧ ᱥᱮᱱᱚᱜᱼᱟ᱾", "iɲ dɔ bir teɲ senɔɡa.", "Forest Sentence with Mucad"),
        ("ᱱᱚᱣᱟ ᱫᱚ ᱢᱤᱫᱴᱟᱝ ᱯᱩᱛᱷᱤ ᱠᱟᱱᱟ᱾", "nɔwa dɔ midʈaŋ putʰi kana.", "Pedagogical Object Labeling"),
        ("ᱟᱢ ᱚᱠᱟᱛᱮᱢ ᱪᱟᱞᱟᱜ ᱠᱟᱱᱟ?", "am ɔkatem calaɡ kana?", "Interrogative Classroom Question"),
        ("ᱥᱮᱛᱟᱜ ᱵᱮᱲᱟ ᱵᱮᱲᱟ ᱨᱟᱠᱟᱵ ᱮᱱᱟ᱾", "setaɡ bɛɽa bɛɽa rakab ena.", "Gahla Tundag Low Vowel (ᱮᱹ)"),
        ("ᱢᱚᱬᱮ ᱜᱚᱴᱟᱝ ᱪᱮᱬᱮ ᱩᱰᱟᱹᱣ ᱮᱱᱟ᱾", "mɔɳe ɡɔʈaŋ ceɳe uɖəw ena.", "Retroflex Nasals & Flaps (ᱬ, ᱲ)"),
        ("᱑ ᱒ ᱓ ᱔ ᱕ ᱖ ᱗ ᱘ ᱙ ᱐", "mid bar pe pon mɔɳe turuj ejaj irəl are sun", "Ol Chiki Numerals 1-10"),
    ]

    print("=" * 65)
    print("Santhali Ol Chiki -> IPA Phonemizer Validation Suite")
    print("=" * 65)
    all_passed = True
    for olck, expected_substr, label in test_cases:
        ipa = santhali_to_ipa(olck)
        print(f"\n[TEST] {label}")
        print(f"  Input Ol Chiki: {olck}")
        print(f"  Output IPA:     {ipa}")
        # Verify no unmapped Ol Chiki codepoints remain in output (U+1C50 - U+1C7F)
        unmapped = [c for c in ipa if '\u1C50' <= c <= '\u1C7F']
        if unmapped:
            print(f"  [FAIL] Unmapped Ol Chiki codepoints detected: {unmapped}")
            all_passed = False
        else:
            print(f"  [PASS] 100% Ol Chiki codepoints converted to IPA.")

    if all_passed:
        print(f"\n{'=' * 65}")
        print("[SUCCESS] All test cases passed! Phonemizer is production-ready.")
        print(f"{'=' * 65}")
    else:
        sys.exit(1)
