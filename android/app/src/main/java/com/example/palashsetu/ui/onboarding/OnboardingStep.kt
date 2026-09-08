package com.example.palashsetu.ui.onboarding

import com.example.palashsetu.ui.components.BottomTab

/** Direction the tooltip arrow points toward — i.e. the direction of the spotlight from the tooltip. */
enum class ArrowDirection { TOP, BOTTOM, LEFT, RIGHT }

/** Shape of the spotlight cutout punched through the dim overlay. */
enum class SpotlightShape { CIRCLE, RECT }

/**
 * A single onboarding walkthrough step.
 *
 * @param stepIndex      0-based index in the global 16-step sequence.
 * @param targetScreen   Which [BottomTab] must be active; null = any (e.g. TopBar items).
 * @param targetTag      Matches the string passed to [Modifier.onboardingTarget].
 * @param hiTitle        Hindi heading — compact, ≤ 5 words.
 * @param enTitle        English heading — compact, ≤ 5 words.
 * @param hiBody         Hindi explanation — ≤ 2 plain lines.
 * @param enBody         English explanation — ≤ 2 plain lines.
 * @param arrowDirection Where the tooltip arrow points relative to the tooltip card.
 * @param spotlightShape [SpotlightShape.CIRCLE] for circular targets (FAB), [SpotlightShape.RECT] for cards/buttons.
 */
data class OnboardingStep(
    val stepIndex: Int,
    val targetScreen: BottomTab?,
    val targetTag: String,
    val hiTitle: String,
    val enTitle: String,
    val hiBody: String,
    val enBody: String,
    val arrowDirection: ArrowDirection = ArrowDirection.BOTTOM,
    val spotlightShape: SpotlightShape = SpotlightShape.RECT
)

/** The full ordered walkthrough sequence covering all 3 screens. */
val ALL_ONBOARDING_STEPS: List<OnboardingStep> = listOf(

    // ── SCREEN 1: LIVE VOICE BRIDGE (5 steps) ─────────────────────────────────

    OnboardingStep(
        stepIndex = 0,
        targetScreen = BottomTab.LIVE,
        targetTag = "language_toggle",
        hiTitle = "भाषा बदलें",
        enTitle = "Switch Language",
        hiBody = "यहाँ \"हिन्दी\" या \"English\" दबाएँ — पूरा ऐप उसी भाषा में बदल जाएगा।",
        enBody = "Tap \"हिन्दी\" or \"English\" here — the entire app switches instantly.",
        arrowDirection = ArrowDirection.TOP,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 1,
        targetScreen = BottomTab.LIVE,
        targetTag = "dialect_chips",
        hiTitle = "बोली चुनें",
        enTitle = "Choose Dialect",
        hiBody = "यहाँ से संथाली की बोली चुनें — जैसे उत्तरी संथाली या दक्षिणी संथाली।",
        enBody = "Select the Santali dialect spoken by your students — Northern or Southern.",
        arrowDirection = ArrowDirection.BOTTOM,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 2,
        targetScreen = BottomTab.LIVE,
        targetTag = "hindi_transcript_card",
        hiTitle = "आपका हिंदी वाक्य",
        enTitle = "Your Hindi Sentence",
        hiBody = "जब आप बोलेंगे, तो आपकी हिंदी यहाँ तुरंत दिखेगी।",
        enBody = "As you speak, your Hindi words appear here in real time.",
        arrowDirection = ArrowDirection.BOTTOM,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 3,
        targetScreen = BottomTab.LIVE,
        targetTag = "olchiki_output_card",
        hiTitle = "संथाली अनुवाद",
        enTitle = "Santali Translation",
        hiBody = "अनुवाद यहाँ ओल चिकी लिपि में दिखेगा — बच्चों को दिखाएँ।",
        enBody = "The Ol Chiki translation appears here. Show this to your students.",
        arrowDirection = ArrowDirection.BOTTOM,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 4,
        targetScreen = BottomTab.LIVE,
        targetTag = "play_audio_button",
        hiTitle = "बच्चों को सुनाएँ",
        enTitle = "Play for Students",
        hiBody = "\"सुनाएं\" बटन दबाएँ — Piper TTS संथाली में बोल देगा।",
        enBody = "Press \"Listen\" — Piper TTS will speak the Santali translation aloud.",
        arrowDirection = ArrowDirection.TOP,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 5,
        targetScreen = BottomTab.LIVE,
        targetTag = "mic_fab",
        hiTitle = "बोलें — अनुवाद करें",
        enTitle = "Speak to Translate",
        hiBody = "इस नीले माइक बटन को दबाएँ और हिंदी में बोलें।\nछोड़ने पर संथाली में अनुवाद होगा।",
        enBody = "Press & hold this mic button, then speak in Hindi.\nRelease to get the Santali translation.",
        arrowDirection = ArrowDirection.TOP,
        spotlightShape = SpotlightShape.CIRCLE
    ),

    // ── SCREEN 2: FLN PHRASEBOOK / शब्दावली (4 steps) ────────────────────────

    OnboardingStep(
        stepIndex = 6,
        targetScreen = BottomTab.PHRASEBOOK,
        targetTag = "phrasebook_tab",
        hiTitle = "शब्दावली देखें",
        enTitle = "Open FLN Bank",
        hiBody = "\"शब्दावली\" टैब दबाएँ — 368 से अधिक NIPUN भारत वाक्यांश मिलेंगे।",
        enBody = "Tap the \"FLN Bank\" tab to browse 368+ NIPUN Bharat classroom phrases.",
        arrowDirection = ArrowDirection.TOP,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 7,
        targetScreen = BottomTab.PHRASEBOOK,
        targetTag = "phrasebook_search_bar",
        hiTitle = "कमांड खोजें",
        enTitle = "Search Phrases",
        hiBody = "यहाँ हिंदी या अंग्रेज़ी में टाइप करें — जरूरी वाक्य तुरंत मिलेगा।",
        enBody = "Type in Hindi or English — find the exact phrase you need instantly.",
        arrowDirection = ArrowDirection.BOTTOM,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 8,
        targetScreen = BottomTab.PHRASEBOOK,
        targetTag = "phrasebook_category_chips",
        hiTitle = "विषय से छाँटें",
        enTitle = "Filter by Category",
        hiBody = "कक्षा प्रबंधन, गिनती, अभिवादन — किसी भी श्रेणी से वाक्य देखें।",
        enBody = "Browse by Classroom, Numbers, Greetings, or any category.",
        arrowDirection = ArrowDirection.BOTTOM,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 9,
        targetScreen = BottomTab.PHRASEBOOK,
        targetTag = "phrasebook_first_card",
        hiTitle = "वाक्यांश पर टैप करें",
        enTitle = "Tap a Phrase Card",
        hiBody = "किसी भी कार्ड पर टैप करें — संथाली TTS तुरंत बोल देगा।",
        enBody = "Tap any phrase card — Santali TTS speaks it aloud immediately.",
        arrowDirection = ArrowDirection.TOP,
        spotlightShape = SpotlightShape.RECT
    ),

    // ── SCREEN 3: PEDAGOGY STUDIO — वर्कशीट + फ्लैशकार्ड (6 steps) ──────────

    OnboardingStep(
        stepIndex = 10,
        targetScreen = BottomTab.STUDIO,
        targetTag = "studio_tab",
        hiTitle = "स्टूडियो खोलें",
        enTitle = "Open Studio",
        hiBody = "\"स्टूडियो\" टैब दबाएँ — वर्कशीट और फ्लैशकार्ड बनाएँ।",
        enBody = "Tap the \"Studio\" tab to create worksheets and flashcards.",
        arrowDirection = ArrowDirection.TOP,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 11,
        targetScreen = BottomTab.STUDIO,
        targetTag = "studio_mode_switcher",
        hiTitle = "मोड चुनें",
        enTitle = "Choose Studio Mode",
        hiBody = "\"कार्यपत्रक\" = PDF वर्कशीट।\n\"फ्लैशकार्ड\" = शब्द कार्ड।",
        enBody = "\"Worksheets\" = printable PDFs.\n\"Flashcards\" = visual word cards.",
        arrowDirection = ArrowDirection.BOTTOM,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 12,
        targetScreen = BottomTab.STUDIO,
        targetTag = "studio_grade_selector",
        hiTitle = "कक्षा चुनें",
        enTitle = "Select Grade",
        hiBody = "कक्षा 1, 2, या 3 चुनें — उस कक्षा के NIPUN दक्षता लक्ष्य दिखेंगे।",
        enBody = "Choose Grade 1, 2, or 3 — NIPUN competency targets for that grade appear.",
        arrowDirection = ArrowDirection.BOTTOM,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 13,
        targetScreen = BottomTab.STUDIO,
        targetTag = "studio_competency_chip",
        hiTitle = "दक्षता लक्ष्य बदलें",
        enTitle = "Change Competency",
        hiBody = "इस चिप पर टैप करें — 24 NIPUN भारत दक्षता लक्ष्यों में से चुनें।",
        enBody = "Tap this chip to choose from 24 NIPUN Bharat learning competencies.",
        arrowDirection = ArrowDirection.BOTTOM,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 14,
        targetScreen = BottomTab.STUDIO,
        targetTag = "studio_generate_button",
        hiTitle = "वर्कशीट बनाएँ",
        enTitle = "Generate Worksheet",
        hiBody = "\"PDF बनाएं\" दबाएँ — कुछ सेकंड में वर्कशीट तैयार होगी।",
        enBody = "Press \"Generate PDF\" — your bilingual worksheet is ready in seconds.",
        arrowDirection = ArrowDirection.TOP,
        spotlightShape = SpotlightShape.RECT
    ),

    OnboardingStep(
        stepIndex = 15,
        targetScreen = BottomTab.STUDIO,
        targetTag = "studio_flashcard_section",
        hiTitle = "फ्लैशकार्ड स्टूडियो",
        enTitle = "Flashcard Studio",
        hiBody = "\"फ्लैशकार्ड\" मोड में शब्द कार्ड देखें, सुनें और प्रिंट करें।",
        enBody = "In Flashcard mode — view, listen to, and print visual word cards.",
        arrowDirection = ArrowDirection.TOP,
        spotlightShape = SpotlightShape.RECT
    )
)
