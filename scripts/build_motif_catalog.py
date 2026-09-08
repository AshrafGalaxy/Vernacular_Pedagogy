"""
Generates the comprehensive, culturally authentic Santhali-Hindi bilingual
motif catalog for all 150 visual realia assets.
"""

import json
import os

MANIFEST_PATH = os.path.join("data", "processed", "motif_assets_manifest.json")
OUTPUT_PATH = os.path.join("android", "app", "src", "main", "assets", "schemas", "motif_catalog.json")

# Master bilingual pedagogical lexicon mapping for all 150 motifs
LEXICON = {
    "motif_ant": {
        "nameHi": "चींटी",
        "nameOlChiki": "ᱢᱩᱡᱽ",
        "phoneticDeva": "[मुज]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["चींटी", "ant", "मुज", "कीट", "छोटे जीव"],
        "contextPrompts": ["छोटे जीव", "कीट पतंग", "प्रकृति", "परिश्रम"]
    },
    "motif_back": {
        "nameHi": "पीठ",
        "nameOlChiki": "ᱫᱮᱭᱟ",
        "phoneticDeva": "[देया]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["पीठ", "back", "देया", "शरीर"],
        "contextPrompts": ["शरीर के अंग", "मानव शरीर"]
    },
    "motif_banana": {
        "nameHi": "केला",
        "nameOlChiki": "ᱠᱟᱭᱨᱟ",
        "phoneticDeva": "[कायरा]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["केला", "banana", "कायरा", "फल", "पीला केला"],
        "contextPrompts": ["फल और सब्जियां", "पोषण", "स्वास्थ्य", "मीठे फल"]
    },
    "motif_bear": {
        "nameHi": "भालू",
        "nameOlChiki": "ᱵᱟᱱᱟ",
        "phoneticDeva": "[बाना]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["भालू", "bear", "बाना", "जंगली जानवर"],
        "contextPrompts": ["जंगली जानवर", "जंगल के जीव", "वन्य प्राणी"]
    },
    "motif_ber_fruit": {
        "nameHi": "बेर",
        "nameOlChiki": "ᱡᱟᱱᱩᱢ ᱡᱚ",
        "phoneticDeva": "[जानुम जो]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["बेर", "berry", "जानुम", "फल", "झाड़ी"],
        "contextPrompts": ["स्थानीय फल", "झारखंड के फल", "जंगली बेर"]
    },
    "motif_big_small_pots": {
        "nameHi": "बड़ा और छोटा घड़ा",
        "nameOlChiki": "ᱢᱟᱨᱟᱝ ᱟᱨ ᱦᱩᱰᱤᱧ ᱴᱩᱠᱩᱡ",
        "phoneticDeva": "[मारांग आर हुडिंज टुकुज]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["बड़ा छोटा", "तुलना", "बर्तन", "घड़ा", "shapes", "comparison"],
        "contextPrompts": ["आकार की तुलना", "बड़ा और छोटा", "आकृतियां"]
    },
    "motif_bird_in_sky": {
        "nameHi": "आकाश में पक्षी",
        "nameOlChiki": "ᱥᱮᱨᱢᱟ ᱨᱮ ᱪᱮᱬᱮ",
        "phoneticDeva": "[सेरमा रे चेणॆ]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["पक्षी", "चिड़िया", "आकाश", "bird", "उड़ना"],
        "contextPrompts": ["पक्षी और आकाश", "उड़ने वाले जीव", "नीला आकाश"]
    },
    "motif_bitter_gourd": {
        "nameHi": "करेला",
        "nameOlChiki": "ᱠᱟᱨᱞᱟ",
        "phoneticDeva": "[कारला]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 2,
        "tags": ["करेला", "bitter gourd", "कारला", "सब्जी"],
        "contextPrompts": ["सब्जियों के नाम", "स्वाद", "कड़वा"]
    },
    "motif_blackboard": {
        "nameHi": "श्यामपट्ट (ब्लैकबोर्ड)",
        "nameOlChiki": "ᱦᱮᱸᱫᱮ ᱯᱟᱴᱷᱟ",
        "phoneticDeva": "[हेंदे पाठा]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["ब्लैकबोर्ड", "श्यामपट्ट", "blackboard", "कक्षा"],
        "contextPrompts": ["कक्षा की वस्तुएं", "विद्यालय", "पढ़ाई"]
    },
    "motif_book": {
        "nameHi": "पुस्तक / किताब",
        "nameOlChiki": "ᱯᱩᱛᱷᱤ",
        "phoneticDeva": "[पुथी]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["किताब", "पुस्तक", "book", "पुथी"],
        "contextPrompts": ["पढ़ाई लिखाई", "कक्षा", "साक्षरता"]
    },
    "motif_bottle_gourd": {
        "nameHi": "लौकी / कद्दू",
        "nameOlChiki": "ᱦᱚᱛᱚᱫ",
        "phoneticDeva": "[होतोद]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["लौकी", "कद्दू", "bottle gourd", "होतोद"],
        "contextPrompts": ["हरी सब्जियां", "खेती", "पोषण"]
    },
    "motif_buffalo": {
        "nameHi": "भैंस",
        "nameOlChiki": "ᱠᱟᱰᱟ",
        "phoneticDeva": "[काडा]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["भैंस", "buffalo", "काडा", "पालतू पशु"],
        "contextPrompts": ["पालतू पशु", "ग्रामीण जीवन", "दूध"]
    },
    "motif_butterfly": {
        "nameHi": "तितली",
        "nameOlChiki": "ᱯᱤᱯᱤᱲᱤᱭᱟᱹᱝ",
        "phoneticDeva": "[पिपिड़ियांग]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["तितली", "butterfly", "पिपिड़ियांग", "रंग बिरंगी"],
        "contextPrompts": ["सुंदर कीट", "रंग बिरंगी तितली", "उड़ान"]
    },
    "motif_butterfly_on_flower": {
        "nameHi": "फूल पर तितली",
        "nameOlChiki": "ᱵᱟᱦᱟ ᱨᱮ ᱯᱤᱯᱤᱲᱤᱭᱟᱹᱝ",
        "phoneticDeva": "[बाहा रे पिपिड़ियांग]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["फूल और तितली", "फूल", "baha", "प्रकृति"],
        "contextPrompts": ["प्रकृति और परिवेश", "फूल", "सुंदर दृश्य"]
    },
    "motif_calf": {
        "nameHi": "बछड़ा",
        "nameOlChiki": "ᱫᱟᱢᱲᱟ",
        "phoneticDeva": "[दामड़ा]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["बछड़ा", "calf", "दामड़ा", "गाय का बच्चा"],
        "contextPrompts": ["पशु और उनके बच्चे", "पालतू जीव"]
    },
    "motif_cat": {
        "nameHi": "बिल्ली",
        "nameOlChiki": "ᱵᱤᱞᱟᱹᱭ",
        "phoneticDeva": "[बिलाई]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["बिल्ली", "cat", "बिलाई", "पुसी"],
        "contextPrompts": ["पालतू जानवर", "घर के जीव"]
    },
    "motif_chair": {
        "nameHi": "कुर्सी",
        "nameOlChiki": "ᱢᱟᱹᱪᱤ",
        "phoneticDeva": "[माची]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["कुर्सी", "chair", "माची", "फर्नीचर"],
        "contextPrompts": ["कक्षा का फर्नीचर", "बैठने की जगह"]
    },
    "motif_chalk": {
        "nameHi": "खली (चाक)",
        "nameOlChiki": "ᱪᱚᱠ",
        "phoneticDeva": "[चोक]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["चाक", "chalk", "लिखना", "सफेद"],
        "contextPrompts": ["लिखने की सामग्री", "कक्षा", "शिक्षक"]
    },
    "motif_chest": {
        "nameHi": "छाती",
        "nameOlChiki": "ᱠᱚᱲᱟᱢ",
        "phoneticDeva": "[कोड़ाम]",
        "category": "BODY_PARTS",
        "grade": 2,
        "tags": ["छाती", "chest", "कोड़ाम", "शरीर"],
        "contextPrompts": ["शरीर के अंग", "स्वास्थ्य"]
    },
    "motif_children_ball_game": {
        "nameHi": "गेंद का खेल",
        "nameOlChiki": "ᱵᱚᱞ ᱮᱱᱮᱡ",
        "phoneticDeva": "[बोल एनेज]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["गेंद खेल", "खेलकूद", "ball game", "साथी"],
        "contextPrompts": ["खेल और व्यायाम", "मित्रता", "मैदान"]
    },
    "motif_child_giving_object": {
        "nameHi": "वस्तु देना",
        "nameOlChiki": "ᱡᱤᱱᱤᱥ ᱮᱢ",
        "phoneticDeva": "[जिनिस एम]",
        "category": "PEOPLE_ACTIONS",
        "grade": 2,
        "tags": ["देना", "सहयोग", "give object", "मदद"],
        "contextPrompts": ["सद्गुण और सहयोग", "क्रिया", "सहानुभूति"]
    },
    "motif_child_listening": {
        "nameHi": "ध्यान से सुनना",
        "nameOlChiki": "ᱟᱸᱡᱚᱢ",
        "phoneticDeva": "[आंजोम]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["सुनना", "listening", "आंजोम", "ध्यान"],
        "contextPrompts": ["कक्षा निर्देश", "श्रवण कौशल", "ध्यान"]
    },
    "motif_child_playing": {
        "nameHi": "खेलना",
        "nameOlChiki": "ᱮᱱᱮᱡ",
        "phoneticDeva": "[एनेज]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["खेलना", "playing", "एनेज", "खुशी"],
        "contextPrompts": ["दैनिक क्रियाएं", "आनंद", "बाल सुलभ खेल"]
    },
    "motif_child_reading": {
        "nameHi": "पढ़ना",
        "nameOlChiki": "ᱯᱟᱲᱦᱟᱣ",
        "phoneticDeva": "[पड़हाव]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["पढ़ना", "reading", "पड़हाव", "किताब"],
        "contextPrompts": ["साक्षरता", "पढ़ने की आदत", "विद्या"]
    },
    "motif_child_showing_object": {
        "nameHi": "वस्तु दिखाना",
        "nameOlChiki": "ᱩᱫᱩᱜ",
        "phoneticDeva": "[उदुक]",
        "category": "PEOPLE_ACTIONS",
        "grade": 2,
        "tags": ["दिखाना", "show object", "उदुक", "प्रदर्शन"],
        "contextPrompts": ["कक्षा गतिविधि", "दिखाना"]
    },
    "motif_child_sitting": {
        "nameHi": "बैठना",
        "nameOlChiki": "ᱫᱩᱲᱩᱵ",
        "phoneticDeva": "[दुड़ुब]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["बैठना", "sit", "दुड़ुब", "आसन"],
        "contextPrompts": ["कक्षा अनुशासन", "आसन", "शांति"]
    },
    "motif_child_speaking": {
        "nameHi": "बोलना",
        "nameOlChiki": "ᱨᱚᱲ",
        "phoneticDeva": "[रोड़]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["बोलना", "speak", "रोड़", "बातचीत"],
        "contextPrompts": ["मौखिक अभिव्यक्ति", "संवाद", "वार्तालाप"]
    },
    "motif_child_standing": {
        "nameHi": "खड़ा होना",
        "nameOlChiki": "ᱛᱤᱸᱜᱩ",
        "phoneticDeva": "[तिंगु]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["खड़ा होना", "stand", "तिंगु", "सावधान"],
        "contextPrompts": ["कक्षा शिष्टाचार", "खड़ा होना"]
    },
    "motif_child_writing": {
        "nameHi": "लिखना",
        "nameOlChiki": "ᱚᱞ",
        "phoneticDeva": "[ओल]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["लिखना", "write", "ओल", "कलम"],
        "contextPrompts": ["लेखन कौशल", "अभ्यास", "अक्षर ज्ञान"]
    },
    "motif_circle_shape": {
        "nameHi": "वृत्त (गोल)",
        "nameOlChiki": "ᱜᱩᱞᱟᱹᱭ",
        "phoneticDeva": "[गुलाई]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["गोला", "वृत्त", "circle", "गुलाई", "आकार"],
        "contextPrompts": ["आकृतियां और आकार", "गोल वस्तुएं"]
    },
    "motif_classroom": {
        "nameHi": "कक्षा कक्ष",
        "nameOlChiki": "ᱪᱟᱱᱟᱪ ᱚᱲᱟᱜ",
        "phoneticDeva": "[चानाच ओड़ाग]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["कक्षा कक्ष", "क्लासरूम", "विद्यालय", "कमरा"],
        "contextPrompts": ["हमारा विद्यालय", "सीखने का स्थान", "कक्षा"]
    },
    "motif_clay_pot_water": {
        "nameHi": "मिट्टी का घड़ा (जल)",
        "nameOlChiki": "ᱦᱟᱥᱟ ᱴᱩᱠᱩᱡ",
        "phoneticDeva": "[हासा टुकुज]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["घड़ा", "जल", "पानी", "pot", "शीतल"],
        "contextPrompts": ["जल संरक्षण", "पारंपरिक वस्तुएं", "पीने का पानी"]
    },
    "motif_cooked_rice_bowl": {
        "nameHi": "भात (पका चावल)",
        "nameOlChiki": "ᱫᱟᱠᱟ",
        "phoneticDeva": "[दाका]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["भात", "चावल", "दाल-भात", "daka", "भोजन"],
        "contextPrompts": ["भोजन", "मध्याह्न भोजन (MDM)", "दैनिक आहार"]
    },
    "motif_count_mango_01": {
        "nameHi": "१ एक आम",
        "nameOlChiki": "ᱢᱤᱫ ᱩᱞ",
        "phoneticDeva": "[मिद उल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["एक", "1", "आम", "mid", "गिनती"],
        "contextPrompts": ["संख्याज्ञान १ से १०", "गिनती 1", "गिनो और लिखो"]
    },
    "motif_count_mango_02": {
        "nameHi": "२ दो आम",
        "nameOlChiki": "ᱵᱟᱨ ᱩᱞ",
        "phoneticDeva": "[बार उल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["दो", "2", "आम", "bar", "गिनती"],
        "contextPrompts": ["संख्याज्ञान १ से १०", "गिनती 2", "जोड़ा"]
    },
    "motif_count_mango_03": {
        "nameHi": "३ तीन आम",
        "nameOlChiki": "ᱯᱮ ᱩᱞ",
        "phoneticDeva": "[पे उल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["तीन", "3", "आम", "pe", "गिनती"],
        "contextPrompts": ["संख्याज्ञान १ से १०", "गिनती 3"]
    },
    "motif_count_mango_04": {
        "nameHi": "४ चार आम",
        "nameOlChiki": "ᱯᱳᱱ ᱩᱞ",
        "phoneticDeva": "[पोन उल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["चार", "4", "आम", "pon", "गिनती"],
        "contextPrompts": ["संख्याज्ञान १ से १०", "गिनती 4"]
    },
    "motif_count_mango_05": {
        "nameHi": "५ पाँच आम",
        "nameOlChiki": "ᱢᱚᱬᱮ ᱩᱞ",
        "phoneticDeva": "[मोणे उल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["पाँच", "5", "आम", "more", "गिनती"],
        "contextPrompts": ["संख्याज्ञान १ से १०", "गिनती 5", "हाथ की उंगलियां"]
    },
    "motif_count_mango_06": {
        "nameHi": "६ छह आम",
        "nameOlChiki": "ᱛᱩᱨᱩᱭ ᱩᱞ",
        "phoneticDeva": "[तुरुय उल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["छह", "6", "आम", "turui", "गिनती"],
        "contextPrompts": ["संख्याज्ञान १ से १०", "गिनती 6"]
    },
    "motif_count_mango_07": {
        "nameHi": "७ सात आम",
        "nameOlChiki": "ᱮᱭᱟᱭ ᱩᱞ",
        "phoneticDeva": "[एयाय उल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["सात", "7", "आम", "eyay", "गिनती"],
        "contextPrompts": ["संख्याज्ञान १ से १०", "गिनती 7", "सप्ताह के दिन"]
    },
    "motif_count_mango_08": {
        "nameHi": "८ आठ आम",
        "nameOlChiki": "ᱤᱨᱟᱹᱞ ᱩᱞ",
        "phoneticDeva": "[इरल उल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["आठ", "8", "आम", "iral", "गिनती"],
        "contextPrompts": ["संख्याज्ञान १ से १०", "गिनती 8"]
    },
    "motif_count_mango_09": {
        "nameHi": "९ नौ आम",
        "nameOlChiki": "ᱟᱨᱮ ᱩᱞ",
        "phoneticDeva": "[आरे उल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["नौ", "9", "आम", "are", "गिनती"],
        "contextPrompts": ["संख्याज्ञान १ से १०", "गिनती 9"]
    },
    "motif_count_mango_10": {
        "nameHi": "१० दस आम",
        "nameOlChiki": "ᱜᱮᱞ ᱩᱞ",
        "phoneticDeva": "[गेल उल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["दस", "10", "आम", "gel", "गिनती"],
        "contextPrompts": ["संख्याज्ञान १ से १०", "गिनती 10", "दहाई"]
    },
    "motif_cow": {
        "nameHi": "गाय",
        "nameOlChiki": "ᱜᱟᱹᱭ",
        "phoneticDeva": "[गाई]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["गाय", "cow", "गाई", "पशु", "दूध"],
        "contextPrompts": ["पालतू पशु", "दुधारू पशु", "गौ माता"]
    },
    "motif_crow": {
        "nameHi": "कौआ",
        "nameOlChiki": "ᱠᱟᱦᱩ",
        "phoneticDeva": "[काहु]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["कौआ", "crow", "काहु", "पक्षी", "काला"],
        "contextPrompts": ["हमारे आसपास के पक्षी", "चतुर कौआ"]
    },
    "motif_cucumber": {
        "nameHi": "खीरा",
        "nameOlChiki": "ᱛᱟᱹᱦᱤᱧ",
        "phoneticDeva": "[ताहिंज]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["खीरा", "cucumber", "ताहिंज", "सलाद"],
        "contextPrompts": ["हरी सब्जियां", "सलाद", "शीतल"]
    },
    "motif_deer": {
        "nameHi": "हिरण",
        "nameOlChiki": "ᱡᱷᱟᱝᱠᱟᱨ",
        "phoneticDeva": "[झांकार]",
        "category": "ANIMALS",
        "grade": 2,
        "tags": ["हिरण", "deer", "झांकार", "तेज दौड़ना"],
        "contextPrompts": ["जंगली जानवर", "वन्य जीव", "सुंदर जीव"]
    },
    "motif_doctor_child": {
        "nameHi": "बाल चिकित्सक (डॉक्टर)",
        "nameOlChiki": "ᱰᱟᱠᱛᱚᱨ ᱜᱤᱫᱽᱨᱟᱹ",
        "phoneticDeva": "[डाकतर गिदरा]",
        "category": "PEOPLE_ACTIONS",
        "grade": 2,
        "tags": ["चिकित्सक", "डॉक्टर", "doctor", "इलाज"],
        "contextPrompts": ["हमारे सहायक", "व्यवसाय", "स्वास्थ्य"]
    },
    "motif_dog": {
        "nameHi": "कुत्ता",
        "nameOlChiki": "ᱥᱮᱛᱟ",
        "phoneticDeva": "[सेता]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["कुत्ता", "dog", "सेता", "वफादार"],
        "contextPrompts": ["पालतू जानवर", "वफादार जीव", "घर की रखवाली"]
    },
    "motif_dry_leaves_pile": {
        "nameHi": "सूखे पत्तों का ढेर",
        "nameOlChiki": "ᱨᱚᱦᱚᱲ ᱥᱟᱠᱟᱢ",
        "phoneticDeva": "[रोहोड़ साकाम]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["सूखे पत्ते", "पतझड़", "sakam", "पत्ते"],
        "contextPrompts": ["प्रकृति और ऋतुएं", "पतझड़", "जंगल"]
    },
    "motif_duck": {
        "nameHi": "बत्तख",
        "nameOlChiki": "ᱜᱮᱰᱮ",
        "phoneticDeva": "[गेडे]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["बत्तख", "duck", "गेडे", "तैरना"],
        "contextPrompts": ["जलचर पक्षी", "तालाब", "तैरने वाले पक्षी"]
    },
    "motif_ears": {
        "nameHi": "कान",
        "nameOlChiki": "ᱞᱩᱛᱩᱨ",
        "phoneticDeva": "[लुतुर]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["कान", "ear", "लुतुर", "सुनना"],
        "contextPrompts": ["ज्ञानेंद्रियां", "शरीर के अंग", "सुनने की शक्ति"]
    },
    "motif_eggplant": {
        "nameHi": "बैंगन",
        "nameOlChiki": "ᱵᱮᱸᱜᱟᱲ",
        "phoneticDeva": "[बेंगाड़]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["बैंगन", "eggplant", "बेंगाड़", "सब्जी"],
        "contextPrompts": ["सब्जियों के नाम", "रसोई"]
    },
    "motif_elephant": {
        "nameHi": "हाथी",
        "nameOlChiki": "ᱦᱟᱹᱛᱤ",
        "phoneticDeva": "[हाती]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["हाथी", "elephant", "हाती", "विशाल"],
        "contextPrompts": ["विशाल वन्य जीव", "जंगल", "झारखंड का राजकीय पशु"]
    },
    "motif_eyes": {
        "nameHi": "आँखें",
        "nameOlChiki": "ᱢᱮᱫ",
        "phoneticDeva": "[मेद]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["आँख", "eye", "मेद", "देखना"],
        "contextPrompts": ["ज्ञानेंद्रियां", "शरीर के अंग", "दृष्टि"]
    },
    "motif_farmer_field": {
        "nameHi": "किसान (खेत)",
        "nameOlChiki": "ᱪᱟᱹᱥᱤ",
        "phoneticDeva": "[चासी]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["किसान", "खेत", "farmer", "chasi", "मेहनत"],
        "contextPrompts": ["अन्नदाता", "खेती किसानी", "हमारा गांव"]
    },
    "motif_feet": {
        "nameHi": "पैर",
        "nameOlChiki": "ᱡᱟᱸᱜᱟ",
        "phoneticDeva": "[जांगा]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["पैर", "feet", "जांगा", "चलना"],
        "contextPrompts": ["शरीर के अंग", "चलना", "दौड़ना"]
    },
    "motif_finger": {
        "nameHi": "उँगली",
        "nameOlChiki": "ᱠᱟᱹᱴᱩᱵ",
        "phoneticDeva": "[काटुब]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["उँगली", "finger", "काटुब", "हाथ"],
        "contextPrompts": ["हाथ की उँगलियाँ", "गिनती", "इशारा"]
    },
    "motif_fingernails": {
        "nameHi": "नाखून",
        "nameOlChiki": "ᱨᱟᱢᱟ",
        "phoneticDeva": "[रामा]",
        "category": "BODY_PARTS",
        "grade": 2,
        "tags": ["नाखून", "nails", "रामा", "सफाई"],
        "contextPrompts": ["स्वच्छता और स्वास्थ्य", "नाखून काटना"]
    },
    "motif_fish": {
        "nameHi": "मछली",
        "nameOlChiki": "ᱦᱟᱠᱳ",
        "phoneticDeva": "[हाको]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["मछली", "fish", "हाको", "जलचर"],
        "contextPrompts": ["जलीय जीव", "तालाब", "जल ही जीवन है"]
    },
    "motif_fish_pond": {
        "nameHi": "मछली का तालाब",
        "nameOlChiki": "ᱦᱟᱠᱳ ᱯᱩᱠᱷᱨᱤ",
        "phoneticDeva": "[हाको पुखरी]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["तालाब", "pond", "मछली", "पानी"],
        "contextPrompts": ["जलस्रोत", "गांव का परिवेश", "तालाब"]
    },
    "motif_frog": {
        "nameHi": "मेंढक",
        "nameOlChiki": "ᱨᱳᱴᱮ",
        "phoneticDeva": "[रोटे]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["मेंढक", "frog", "रोटे", "फुदकना"],
        "contextPrompts": ["वर्षा ऋतु के जीव", "तालाब के किनारे"]
    },
    "motif_garlic": {
        "nameHi": "लहसुन",
        "nameOlChiki": "ᱨᱟᱹᱥᱩᱬ",
        "phoneticDeva": "[रासुण]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 2,
        "tags": ["लहसुन", "garlic", "रासुण", "मसाले"],
        "contextPrompts": ["रसोई और मसाले", "स्वास्थ्य"]
    },
    "motif_ginger": {
        "nameHi": "अदरक",
        "nameOlChiki": "ᱟᱫᱟ",
        "phoneticDeva": "[आदा]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 2,
        "tags": ["अदरक", "ginger", "आदा", "मसाला"],
        "contextPrompts": ["रसोई और औषधि", "घरेलू नुस्खे"]
    },
    "motif_girls_playing": {
        "nameHi": "बालिकाओं का खेल",
        "nameOlChiki": "ᱠᱩᱲᱤ ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚ ᱮᱱᱮᱡ",
        "phoneticDeva": "[कुड़ी गिदरा को एनेज]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["बालिकाएं", "खेल", "साथी", "girls"],
        "contextPrompts": ["मित्रता और खेल", "लड़कियों की शिक्षा"]
    },
    "motif_goat": {
        "nameHi": "बकरी",
        "nameOlChiki": "ᱢᱮᱨᱚᱢ",
        "phoneticDeva": "[मेरोम]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["बकरी", "goat", "मेरोम", "घास"],
        "contextPrompts": ["पालतू पशु", "घरेलू जीव", "गांव"]
    },
    "motif_grandparent_story": {
        "nameHi": "दादा-दादी की कहानी",
        "nameOlChiki": "ᱦᱟᱲᱟᱢ ᱵᱩᱰᱷᱤ ᱠᱟᱹᱦᱱᱤ",
        "phoneticDeva": "[हाड़ाम बुढी काहनी]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["कहानी", "दादा-दादी", "story", "संस्कार"],
        "contextPrompts": ["परिवार और संस्कार", "कहानी सुनना", "बुजुर्गों का आदर"]
    },
    "motif_green_chilli": {
        "nameHi": "हरी मिर्च",
        "nameOlChiki": "ᱵᱮᱨᱮᱞ ᱢᱟᱹᱨᱤᱪ",
        "phoneticDeva": "[बेरेल मारीच]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["मिर्च", "chilli", "मारीच", "तीखा"],
        "contextPrompts": ["सब्जियां और स्वाद", "तीखा स्वाद"]
    },
    "motif_green_sal_leaf": {
        "nameHi": "हरा सखुआ (साल) पत्ता",
        "nameOlChiki": "ᱦᱟᱹᱨᱤᱭᱟᱹᱲ ᱥᱟᱨᱡᱚᱢ ᱥᱟᱠᱟᱢ",
        "phoneticDeva": "[हरियाड़ सारजोम साकाम]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["साल पत्ता", "सखुआ", "sal leaf", "सारजोम", "पवित्र"],
        "contextPrompts": ["झारखंड का राजकीय वृक्ष", "संस्कृति", "सरहुल"]
    },
    "motif_guava": {
        "nameHi": "अमरूद",
        "nameOlChiki": "ᱟᱢᱨᱩᱫᱽ",
        "phoneticDeva": "[अमरुद]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["अमरूद", "guava", "फल", "मीठा"],
        "contextPrompts": ["मौसमी फल", "विटामिन", "बाग"]
    },
    "motif_hair": {
        "nameHi": "बाल",
        "nameOlChiki": "ᱩᱵ",
        "phoneticDeva": "[उब]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["बाल", "hair", "उब", "सिर"],
        "contextPrompts": ["शरीर के अंग", "सफाई", "कंघी"]
    },
    "motif_hands": {
        "nameHi": "हाथ",
        "nameOlChiki": "ᱛᱤ",
        "phoneticDeva": "[ती]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["हाथ", "hands", "ती", "काम करना"],
        "contextPrompts": ["शरीर के अंग", "हाथ धोना", "कर्म"]
    },
    "motif_hand_washing": {
        "nameHi": "हाथ धोना",
        "nameOlChiki": "ᱛᱤ ᱟᱹᱨᱩᱵ",
        "phoneticDeva": "[ती आरुब]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["हाथ धोना", "hand wash", "स्वच्छता", "साबुन"],
        "contextPrompts": ["स्वास्थ्य और स्वच्छता", "भोजन से पहले", "बीमारी से बचाव"]
    },
    "motif_head": {
        "nameHi": "सिर",
        "nameOlChiki": "ᱵᱚᱦᱚᱜ",
        "phoneticDeva": "[बोहोक]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["सिर", "head", "बोहोक", "मस्तिष्क"],
        "contextPrompts": ["शरीर के अंग", "सोचना"]
    },
    "motif_heavy_light_objects": {
        "nameHi": "भारी और हल्का",
        "nameOlChiki": "ᱦᱟᱢᱟᱞ ᱟᱨ ᱨᱟᱣᱟᱞ",
        "phoneticDeva": "[हामाल आर रावाल]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["भारी", "हल्का", "वजन", "heavy light", "तुलना"],
        "contextPrompts": ["माप और तुलना", "वजन", "भौतिक समझ"]
    },
    "motif_hen": {
        "nameHi": "मुर्गी",
        "nameOlChiki": "ᱥᱤᱢ",
        "phoneticDeva": "[सीम]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["मुर्गी", "hen", "सीम", "पक्षी"],
        "contextPrompts": ["घरेलू पक्षी", "अंडे"]
    },
    "motif_honeybee": {
        "nameHi": "मधुमक्खी",
        "nameOlChiki": "ᱧᱮᱞᱮ",
        "phoneticDeva": "[ञेले]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["मधुमक्खी", "honeybee", "ञेले", "शहद"],
        "contextPrompts": ["उपयोगी कीट", "शहद", "फूलों का रस"]
    },
    "motif_honeycomb": {
        "nameHi": "मधुमक्खी का छत्ता",
        "nameOlChiki": "ᱧᱮᱞᱮ ᱪᱷᱟᱛᱟ",
        "phoneticDeva": "[ञेले छाता]",
        "category": "NATURE",
        "grade": 2,
        "tags": ["छत्ता", "honeycomb", "शहद", "मोम"],
        "contextPrompts": ["प्रकृति के अजूबे", "जंगल"]
    },
    "motif_horse": {
        "nameHi": "घोड़ा",
        "nameOlChiki": "ᱥᱟᱫᱚᱢ",
        "phoneticDeva": "[सादोम]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["घोड़ा", "horse", "सादोम", "सवारी"],
        "contextPrompts": ["पशु", "सवारी", "तेज दौड़"]
    },
    "motif_housefly": {
        "nameHi": "मक्खी",
        "nameOlChiki": "ᱨᱳ",
        "phoneticDeva": "[रो]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["मक्खी", "housefly", "रो", "कीट"],
        "contextPrompts": ["कीट पतंग", "रोग और सफाई", "भोजन को ढंकना"]
    },
    "motif_jackfruit": {
        "nameHi": "कटहल",
        "nameOlChiki": "ᱠᱟᱱᱴᱷᱟᱲ",
        "phoneticDeva": "[कांठाड़]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["कटहल", "jackfruit", "कांठाड़", "सब्जी"],
        "contextPrompts": ["झारखंड का फल", "सब्जी", "बड़ा फल"]
    },
    "motif_jaggery_piece": {
        "nameHi": "गुड़ का टुकड़ा",
        "nameOlChiki": "ᱜᱩᱲ",
        "phoneticDeva": "[गुड़]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["गुड़", "jaggery", "मीठा", "गन्ना"],
        "contextPrompts": ["पारंपरिक आहार", "ऊर्जा", "स्वास्थ्य"]
    },
    "motif_jamun": {
        "nameHi": "जामुन",
        "nameOlChiki": "ᱠᱩᱫᱽ",
        "phoneticDeva": "[कुद]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["जामुन", "jamun", "कुद", "फल", "बैंगनी"],
        "contextPrompts": ["जंगली फल", "वनोपज", "वर्षा ऋतु"]
    },
    "motif_leafy_greens": {
        "nameHi": "हरी साग",
        "nameOlChiki": "ᱟᱲᱟᱜ",
        "phoneticDeva": "[आड़ाक]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["साग", "greens", "आड़ाक", "पत्तेदार"],
        "contextPrompts": ["पारंपरिक साग", "स्वास्थ्य", "हरी सब्जियां"]
    },
    "motif_lentil_bowl": {
        "nameHi": "दाल की कटोरी",
        "nameOlChiki": "ᱫᱟᱹᱞ",
        "phoneticDeva": "[दाल]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["दाल", "lentil", "दाल", "कटोरी"],
        "contextPrompts": ["पौष्टिक भोजन", "दाल-भात", "प्रोटीन"]
    },
    "motif_long_short_rope": {
        "nameHi": "लंबी और छोटी रस्सी",
        "nameOlChiki": "ᱡᱤᱞᱤᱧ ᱟᱨ ᱠᱷᱟᱴᱚ ᱵᱟᱵᱮᱨ",
        "phoneticDeva": "[जिलिंज आर खाटो बाबेर]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["लंबा छोटा", "रस्सी", "लंबाई", "long short", "तुलना"],
        "contextPrompts": ["माप और तुलना", "लंबाई", "आकार"]
    },
    "motif_mahua_fruit": {
        "nameHi": "महुआ फल",
        "nameOlChiki": "ᱢᱟᱹᱦᱩᱣᱟᱹ ᱡᱚ",
        "phoneticDeva": "[माहुवा जो]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["महुआ", "mahua", "फल", "पेड़"],
        "contextPrompts": ["संथाली संस्कृति", "वनोपज", "महुआ फल"]
    },
    "motif_mahua_tree": {
        "nameHi": "महुआ का पेड़",
        "nameOlChiki": "ᱢᱟᱹᱦᱩᱣᱟᱹ ᱫᱟᱨᱮ",
        "phoneticDeva": "[माहुवा दारे]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["महुआ पेड़", "वृक्ष", "dare", "छांव"],
        "contextPrompts": ["पेड़-पौधे", "हमारा पर्यावरण", "पवित्र वृक्ष"]
    },
    "motif_mango": {
        "nameHi": "आम",
        "nameOlChiki": "ᱩᱞ",
        "phoneticDeva": "[उल]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["आम", "mango", "उल", "फलों का राजा"],
        "contextPrompts": ["मीठे फल", "गर्मी का मौसम", "फलों का राजा"]
    },
    "motif_milk_cup": {
        "nameHi": "दूध का कटोरा",
        "nameOlChiki": "ᱛᱳᱣᱟ",
        "phoneticDeva": "[तोवा]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["दूध", "milk", "तोवा", "सफेद"],
        "contextPrompts": ["संपूर्ण आहार", "स्वास्थ्य", "मजबूत हड्डियां"]
    },
    "motif_monkey": {
        "nameHi": "बंदर",
        "nameOlChiki": "ᱜᱟᱹᱰᱤ",
        "phoneticDeva": "[गाड़ी]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["बंदर", "monkey", "गाड़ी", "कूदना"],
        "contextPrompts": ["पेड़ों पर रहने वाले जीव", "शरारती जीव"]
    },
    "motif_moon_night": {
        "nameHi": "रात का चाँद",
        "nameOlChiki": "ᱧᱤᱫᱟᱹ ᱪᱟᱸᱫᱚ",
        "phoneticDeva": "[ञिदा चांदो]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["चाँद", "चंद्रमा", "रात", "moon", "प्रकाश"],
        "contextPrompts": ["रात और आकाश", "प्रकृति", "चाँदनी"]
    },
    "motif_mother_child": {
        "nameHi": "माँ और बच्चा",
        "nameOlChiki": "ᱟᱭᱳ ᱟᱨ ᱜᱤᱫᱽᱨᱟᱹ",
        "phoneticDeva": "[आयो आर गिदरा]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["माँ और बच्चा", "ममता", "परिवार", "स्नेह"],
        "contextPrompts": ["हमारा परिवार", "प्यार", "ममता"]
    },
    "motif_mountain": {
        "nameHi": "पहाड़ / पर्वत",
        "nameOlChiki": "ᱵᱩᱨᱩ",
        "phoneticDeva": "[बुरु]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["पहाड़", "पर्वत", "mountain", "बुरु", "ऊंचा"],
        "contextPrompts": ["झारखंड की प्रकृति", "पहाड़", "मरांग बुरु"]
    },
    "motif_mouse": {
        "nameHi": "चूहा",
        "nameOlChiki": "ᱪᱩᱴᱤᱭᱟᱹ",
        "phoneticDeva": "[चुटिया]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["चूहा", "mouse", "चुटिया", "बिल"],
        "contextPrompts": ["छोटे जानवर", "कुतरना"]
    },
    "motif_mouth": {
        "nameHi": "मुँह",
        "nameOlChiki": "ᱢᱚᱪᱟ",
        "phoneticDeva": "[मोचा]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["मुँह", "mouth", "मोचा", "खाना", "बोलना"],
        "contextPrompts": ["शरीर के अंग", "बोलना", "भोजन"]
    },
    "motif_mushroom": {
        "nameHi": "मशरूम (खुखड़ी)",
        "nameOlChiki": "ᱚᱛ",
        "phoneticDeva": "[ओत]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 2,
        "tags": ["खुखड़ी", "मशरूम", "mushroom", "रुगड़ा", "ot"],
        "contextPrompts": ["झारखंड के व्यंजन", "जंगल की खुखड़ी", "वर्षा ऋतु"]
    },
    "motif_mustard_oil_bottle": {
        "nameHi": "सरसों का तेल",
        "nameOlChiki": "ᱛᱩᱲᱤ ᱥᱩᱱᱩᱢ",
        "phoneticDeva": "[तुड़ी सुनुम]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 2,
        "tags": ["सरसों तेल", "mustard oil", "सुनुम", "रसोई"],
        "contextPrompts": ["रसोई और तेल", "मालिश", "स्वास्थ्य"]
    },
    "motif_neck": {
        "nameHi": "गर्दन",
        "nameOlChiki": "ᱦᱚᱴᱚᱜ",
        "phoneticDeva": "[होटोक]",
        "category": "BODY_PARTS",
        "grade": 2,
        "tags": ["गर्दन", "neck", "होटोक", "शरीर"],
        "contextPrompts": ["शरीर के अंग", "गर्दन"]
    },
    "motif_nose": {
        "nameHi": "नाक",
        "nameOlChiki": "ᱢᱩ",
        "phoneticDeva": "[मु]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["नाक", "nose", "मु", "सूंघना"],
        "contextPrompts": ["ज्ञानेंद्रियां", "शरीर के अंग", "सांस लेना"]
    },
    "motif_notebook": {
        "nameHi": "कॉपी / अभ्यास पुस्तिका",
        "nameOlChiki": "ᱚᱞ ᱯᱩᱛᱷᱤ",
        "phoneticDeva": "[ओल पुथी]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["कॉपी", "notebook", "अभ्यास", "लिखना"],
        "contextPrompts": ["कक्षा सामग्री", "लिखना", "गृहकार्य"]
    },
    "motif_older_sister_school": {
        "nameHi": "बड़ी बहन (विद्यालय)",
        "nameOlChiki": "ᱫᱟᱹᱭ",
        "phoneticDeva": "[दाई]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["बड़ी बहन", "दीदी", "sister", "स्कूल"],
        "contextPrompts": ["हमारा परिवार", "विद्यालय जाना", "बड़ी दीदी"]
    },
    "motif_onion": {
        "nameHi": "प्याज",
        "nameOlChiki": "ᱯᱮᱭᱟᱸᱡᱽ",
        "phoneticDeva": "[पेयांज]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["प्याज", "onion", "पेयांज", "सब्जी"],
        "contextPrompts": ["सब्जियों के नाम", "रसोई"]
    },
    "motif_ox": {
        "nameHi": "बैल",
        "nameOlChiki": "ᱰᱟᱝᱜᱽᱨᱟ",
        "phoneticDeva": "[डांगरा]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["बैल", "ox", "डांगरा", "खेती"],
        "contextPrompts": ["पालतू पशु", "खेती किसानी", "मेहनत"]
    },
    "motif_ox_ploughing": {
        "nameHi": "हल जोतता बैल",
        "nameOlChiki": "ᱥᱤ ᱰᱟᱝᱜᱽᱨᱟ",
        "phoneticDeva": "[सी डांगरा]",
        "category": "PEOPLE_ACTIONS",
        "grade": 2,
        "tags": ["हल जोतना", "ploughing", "खेती", "हल"],
        "contextPrompts": ["कृषि कार्य", "मेहनत", "खेत की जुताई"]
    },
    "motif_papaya": {
        "nameHi": "पपीता",
        "nameOlChiki": "ᱯᱚᱯᱮᱭᱟ",
        "phoneticDeva": "[पोपेया]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["पपीता", "papaya", "फल", "पीला"],
        "contextPrompts": ["पौष्टिक फल", "विटामिन", "पाचन"]
    },
    "motif_paper": {
        "nameHi": "कागज",
        "nameOlChiki": "ᱠᱟᱜᱚᱡᱽ",
        "phoneticDeva": "[कागोज]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["कागज", "paper", "कागोज", "सफेद"],
        "contextPrompts": ["लिखने की सामग्री", "कागज की नाव", "कक्षा"]
    },
    "motif_parrot": {
        "nameHi": "तोता",
        "nameOlChiki": "ᱢᱤᱨᱩ",
        "phoneticDeva": "[मीरु]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["तोता", "parrot", "मीरु", "हरा पक्षी"],
        "contextPrompts": ["पक्षी", "सुंदर पक्षी", "लाल चोंच"]
    },
    "motif_peacock": {
        "nameHi": "मोर",
        "nameOlChiki": "ᱢᱟᱨᱟᱜ",
        "phoneticDeva": "[माराक]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["मोर", "peacock", "माराक", "राष्ट्रीय पक्षी"],
        "contextPrompts": ["सुंदर पक्षी", "झारखंड के वन", "मोर पंख", "नृत्य"]
    },
    "motif_pen": {
        "nameHi": "कलम (पेन)",
        "nameOlChiki": "ᱠᱚᱞᱚᱢ",
        "phoneticDeva": "[कोलोम]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["पेन", "कलम", "pen", "कोलोम"],
        "contextPrompts": ["लिखने का साधन", "कक्षा", "स्याही"]
    },
    "motif_pencil": {
        "nameHi": "पेंसिल",
        "nameOlChiki": "ᱯᱮᱱᱥᱤᱞ",
        "phoneticDeva": "[पेन्सिल]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["पेंसिल", "pencil", "चित्रकारी", "रबर"],
        "contextPrompts": ["सीखने की शुरुआत", "कक्षा", "ड्राइंग"]
    },
    "motif_pig": {
        "nameHi": "सूअर",
        "nameOlChiki": "ᱥᱩᱠᱨᱤ",
        "phoneticDeva": "[सुकरी]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["सूअर", "pig", "सुकरी", "जानवर"],
        "contextPrompts": ["घरेलू जानवर", "पशु"]
    },
    "motif_pigeon": {
        "nameHi": "कबूतर",
        "nameOlChiki": "ᱯᱚᱛᱟᱢ",
        "phoneticDeva": "[पोताम]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["कबूतर", "pigeon", "पोताम", "पक्षी"],
        "contextPrompts": ["शांत पक्षी", "पक्षी", "गुटर गूं"]
    },
    "motif_planting_sapling": {
        "nameHi": "पौधा लगाना",
        "nameOlChiki": "ᱫᱟᱨᱮ ᱨᱚᱦᱚᱭ",
        "phoneticDeva": "[दारे रोहोय]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["पौधारोपण", "वृक्षारोपण", "planting", "पौधा"],
        "contextPrompts": ["पर्यावरण संरक्षण", "पेड़ लगाओ", "हरियाली"]
    },
    "motif_potato": {
        "nameHi": "आलू",
        "nameOlChiki": "ᱟᱹᱞᱩ",
        "phoneticDeva": "[आलू]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["आलू", "potato", "आलू", "सब्जी"],
        "contextPrompts": ["दैनिक भोजन", "सब्जियां", "सब्जियों का राजा"]
    },
    "motif_pumpkin": {
        "nameHi": "कद्दू / कोहड़ा",
        "nameOlChiki": "ᱠᱚᱦᱰᱟ",
        "phoneticDeva": "[कोहड़ा]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["कद्दू", "कोहड़ा", "pumpkin", "बड़ा फल"],
        "contextPrompts": ["सब्जियां", "सब्जियों के नाम", "पीला कद्दू"]
    },
    "motif_rabbit": {
        "nameHi": "खरगोश",
        "nameOlChiki": "ᱠᱩᱞᱟᱹᱭ",
        "phoneticDeva": "[कुलाई]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["खरगोश", "rabbit", "कुलाई", "सफेद"],
        "contextPrompts": ["कोमल जीव", "जंगल के जीव", "तेज दौड़"]
    },
    "motif_radish": {
        "nameHi": "मूली",
        "nameOlChiki": "ᱢᱩᱞᱟᱹ",
        "phoneticDeva": "[मूला]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["मूली", "radish", "मूला", "सफेद"],
        "contextPrompts": ["सब्जियां", "सफेद मूली", "सलाद"]
    },
    "motif_rain_cloud": {
        "nameHi": "बारिश के बादल",
        "nameOlChiki": "ᱫᱟᱜ ᱨᱤᱢᱤᱞ",
        "phoneticDeva": "[दाग रिमिल]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["बादल", "वर्षा", "rain cloud", "रिमिल", "पानी"],
        "contextPrompts": ["वर्षा ऋतु", "मौसम", "काले बादल"]
    },
    "motif_raw_rice_grains": {
        "nameHi": "कच्चे चावल के दाने",
        "nameOlChiki": "ᱪᱟᱣᱞᱮ",
        "phoneticDeva": "[चावले]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["चावल", "अक्षत", "rice grains", "अनाज"],
        "contextPrompts": ["अनाज", "हमारा भोजन", "चावल"]
    },
    "motif_red_mango": {
        "nameHi": "लाल/पका आम",
        "nameOlChiki": "ᱟᱨᱟᱜ ᱩᱞ",
        "phoneticDeva": "[आराक उल]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["पका आम", "लाल आम", "मीठा आम", "फल"],
        "contextPrompts": ["फलों की मिठास", "रंग", "स्वादिष्ट आम"]
    },
    "motif_ripe_rice_plant": {
        "nameHi": "पकी धान की फसल",
        "nameOlChiki": "ᱵᱤᱞᱤ ᱦᱳᱲᱳ",
        "phoneticDeva": "[बिली होड़ो]",
        "category": "NATURE",
        "grade": 2,
        "tags": ["धान", "फसल", "rice crop", "होड़ो", "खेत"],
        "contextPrompts": ["कृषि और फसल", "सोना धान", "फसल कटाई"]
    },
    "motif_river": {
        "nameHi": "नदी",
        "nameOlChiki": "ᱜᱟᱰᱟ",
        "phoneticDeva": "[गाडा]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["नदी", "river", "गाडा", "जल", "धारा"],
        "contextPrompts": ["जलस्रोत", "प्रकृति", "स्वर्णरेखा नदी"]
    },
    "motif_rooster": {
        "nameHi": "मुर्गा",
        "nameOlChiki": "ᱥᱟᱺᱰᱤ ᱥᱤᱢ",
        "phoneticDeva": "[सांडी सीम]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["मुर्गा", "rooster", "सांडी सीम", "सुबह"],
        "contextPrompts": ["सुबह की बांग", "पक्षी", "सवेरा"]
    },
    "motif_roti": {
        "nameHi": "रोटी",
        "nameOlChiki": "ᱯᱤᱴᱷᱟᱹ",
        "phoneticDeva": "[पीठा]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["रोटी", "पीठा", "bread", "गेहूं"],
        "contextPrompts": ["दैनिक भोजन", "अनाज", "मां के हाथ की रोटी"]
    },
    "motif_salt_bowl": {
        "nameHi": "नमक की कटोरी",
        "nameOlChiki": "ᱵᱩᱞᱩᱝ",
        "phoneticDeva": "[बुलुंग]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 2,
        "tags": ["नमक", "salt", "बुलुंग", "स्वाद"],
        "contextPrompts": ["स्वाद और स्वास्थ्य", "रसोई"]
    },
    "motif_sal_forest": {
        "nameHi": "साल (सखुआ) का जंगल",
        "nameOlChiki": "ᱥᱟᱨᱡᱚᱢ ᱵᱤᱨ",
        "phoneticDeva": "[सारजोम बीर]",
        "category": "NATURE",
        "grade": 2,
        "tags": ["साल जंगल", "सखुआ वन", "forest", "बीर", "पेड़"],
        "contextPrompts": ["झारखंड के वन", "प्रकृति", "सारंडा वन"]
    },
    "motif_school_bag": {
        "nameHi": "बस्ता / स्कूल बैग",
        "nameOlChiki": "ᱛᱷᱟᱹᱞᱤ",
        "phoneticDeva": "[थाली]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["बस्ता", "स्कूल बैग", "school bag", "किताबें"],
        "contextPrompts": ["विद्यालय की तैयारी", "कक्षा", "बस्ता"]
    },
    "motif_school_bell": {
        "nameHi": "स्कूल की घंटी",
        "nameOlChiki": "ᱟᱥᱲᱟ ᱜᱷᱟᱱᱴᱤ",
        "phoneticDeva": "[आसड़ा घान्टी]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["घंटी", "school bell", "आसड़ा", "समय"],
        "contextPrompts": ["समय चक्र", "कक्षा का समय", "छुट्टी"]
    },
    "motif_school_building": {
        "nameHi": "विद्यालय भवन",
        "nameOlChiki": "ᱟᱥᱲᱟ ᱚᱲᱟᱜ",
        "phoneticDeva": "[आसड़ा ओड़ाक]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["विद्यालय", "स्कूल", "school building", "भवन"],
        "contextPrompts": ["हमारा प्यारा स्कूल", "ज्ञान का मंदिर"]
    },
    "motif_sheep": {
        "nameHi": "भेड़",
        "nameOlChiki": "ᱵᱷᱤᱰᱤ",
        "phoneticDeva": "[भीडी]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["भेड़", "sheep", "भीडी", "ऊन"],
        "contextPrompts": ["पालतू पशु", "ऊन", "घास"]
    },
    "motif_slate": {
        "nameHi": "स्लेट (पाटी)",
        "nameOlChiki": "ᱯᱟᱴᱷᱟ",
        "phoneticDeva": "[पाठा]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["स्लेट", "पाटी", "slate", "पाठा", "पेंसिल"],
        "contextPrompts": ["आरंभिक लेखन", "कक्षा", "वर्णमाला"]
    },
    "motif_small_bird": {
        "nameHi": "छोटी चिड़िया",
        "nameOlChiki": "ᱦᱩᱰᱤᱧ ᱪᱮᱬᱮ",
        "phoneticDeva": "[हुडिंज चेणॆ]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["चिड़िया", "पक्षी", "small bird", "चीं चीं"],
        "contextPrompts": ["छोटे पक्षी", "प्रकृति", "पेड़ पर घोंसला"]
    },
    "motif_snake": {
        "nameHi": "साँप",
        "nameOlChiki": "ᱵᱤᱧ",
        "phoneticDeva": "[बिंज]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["साँप", "snake", "बिंज", "सरीसृप"],
        "contextPrompts": ["सरीसृप जीव", "सावधानी", "जंगल"]
    },
    "motif_square_shape": {
        "nameHi": "वर्ग (चौकोर)",
        "nameOlChiki": "ᱪᱟᱹᱣᱠᱟᱹ",
        "phoneticDeva": "[चावका]",
        "category": "NUMERACY_SHAPES",
        "grade": 1,
        "tags": ["वर्ग", "चौकोर", "square", "चावका", "चार भुजा"],
        "contextPrompts": ["आकृतियां और ज्यामिति", "चौकोर वस्तुएं"]
    },
    "motif_stomach": {
        "nameHi": "पेट",
        "nameOlChiki": "ᱞᱟᱡ",
        "phoneticDeva": "[लाज]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["पेट", "stomach", "लाज", "भूख"],
        "contextPrompts": ["शरीर के अंग", "पाचन", "संतुलित आहार"]
    },
    "motif_sunrise": {
        "nameHi": "सूर्योदय (सवेरा)",
        "nameOlChiki": "ᱵᱮᱲᱟ ᱨᱟᱠᱟᱵ",
        "phoneticDeva": "[बेड़ा राकाब]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["सूर्योदय", "सवेरा", "sunrise", "सूरज", "सुबह"],
        "contextPrompts": ["प्रातः काल", "दिन की शुरुआत", "सूर्य नमस्कार"]
    },
    "motif_table": {
        "nameHi": "मेज (टेबल)",
        "nameOlChiki": "ᱢᱮᱡᱽ",
        "phoneticDeva": "[मेज]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["मेज", "टेबल", "table", "मेज", "कक्षा"],
        "contextPrompts": ["कक्षा का फर्नीचर", "पढ़ाई की मेज"]
    },
    "motif_tamarind_pod": {
        "nameHi": "इमली",
        "nameOlChiki": "ᱡᱚᱡᱚ",
        "phoneticDeva": "[जोजो]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["इमली", "tamarind", "जोजो", "खट्टा"],
        "contextPrompts": ["स्वाद और फल", "पेड़", "खट्टा स्वाद"]
    },
    "motif_teacher_class": {
        "nameHi": "शिक्षक (कक्षा में)",
        "nameOlChiki": "ᱢᱟᱪᱮᱛ",
        "phoneticDeva": "[माचेत]",
        "category": "CLASSROOM",
        "grade": 1,
        "tags": ["शिक्षक", "गुरुजी", "teacher", "माचेत", "गुरु"],
        "contextPrompts": ["आदरणीय शिक्षक", "कक्षा", "मार्गदर्शन"]
    },
    "motif_teeth": {
        "nameHi": "दाँत",
        "nameOlChiki": "ᱰᱟᱴᱟ",
        "phoneticDeva": "[डाटा]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["दाँत", "teeth", "डाटा", "चबाना", "सफेद"],
        "contextPrompts": ["स्वच्छता और स्वास्थ्य", "शरीर के अंग", "ब्रश करना"]
    },
    "motif_tendu_fruit": {
        "nameHi": "केंदू फल",
        "nameOlChiki": "ᱛᱤᱨᱤᱞ ᱡᱚ",
        "phoneticDeva": "[तिरील जो]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 2,
        "tags": ["केंदू फल", "tendu", "तिरील", "मीठा फल"],
        "contextPrompts": ["झारखंड के फल", "वनोपज", "केंदू पत्ता"]
    },
    "motif_tiger": {
        "nameHi": "बाघ",
        "nameOlChiki": "ᱛᱟᱹᱨᱩᱵ",
        "phoneticDeva": "[तारुब]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["बाघ", "tiger", "तारुब", "जंगली जानवर", "धारीदार"],
        "contextPrompts": ["राष्ट्रीय पशु", "जंगल के जीव", "पलामू टाइगर रिजर्व"]
    },
    "motif_tomato": {
        "nameHi": "टमाटर",
        "nameOlChiki": "ᱵᱤᱞᱟᱹᱛᱤ",
        "phoneticDeva": "[बिलाती]",
        "category": "FRUITS_VEG_FOOD",
        "grade": 1,
        "tags": ["टमाटर", "tomato", "बिलाती", "सब्जी", "लाल"],
        "contextPrompts": ["लाल टमाटर", "सब्जियां", "सब्जियों के नाम"]
    },
    "motif_tongue": {
        "nameHi": "जीभ",
        "nameOlChiki": "ᱟᱞᱟᱝ",
        "phoneticDeva": "[आलांग]",
        "category": "BODY_PARTS",
        "grade": 1,
        "tags": ["जीभ", "tongue", "आलांग", "स्वाद"],
        "contextPrompts": ["ज्ञानेंद्रियां", "स्वाद", "बोलना"]
    },
    "motif_turtle": {
        "nameHi": "कछुआ",
        "nameOlChiki": "ᱦᱚᱨᱚ",
        "phoneticDeva": "[होरो]",
        "category": "ANIMALS",
        "grade": 1,
        "tags": ["कछुआ", "turtle", "होरो", "जलचर", "कवच"],
        "contextPrompts": ["जलीय जीव", "धैर्य", "धीमी चाल"]
    },
    "motif_two_friends": {
        "nameHi": "दो मित्र",
        "nameOlChiki": "ᱵᱟᱨ ᱜᱟᱛᱮ",
        "phoneticDeva": "[बार गाते]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["दो दोस्त", "मित्र", "friends", "गाते", "दोस्ती"],
        "contextPrompts": ["मित्रता और सहयोग", "साथ-साथ खेलना", "दोस्ती"]
    },
    "motif_water_lota": {
        "nameHi": "लोटा (जल पात्र)",
        "nameOlChiki": "ᱞᱳᱴᱟ",
        "phoneticDeva": "[लोटा]",
        "category": "NATURE",
        "grade": 1,
        "tags": ["लोटा", "जल पात्र", "पानी", "बर्तन"],
        "contextPrompts": ["पारंपरिक बर्तन", "स्वच्छ जल", "स्वागत"]
    },
    "motif_yellow_rice_stalk": {
        "nameHi": "पीली धान की बाली",
        "nameOlChiki": "ᱥᱟᱥᱟᱝ ᱦᱳᱲᱳ ᱜᱮᱞᱮ",
        "phoneticDeva": "[सासांग होड़ो गेले]",
        "category": "NATURE",
        "grade": 2,
        "tags": ["धान की बाली", "धान", "फसल", "पीली बाली"],
        "contextPrompts": ["कृषि और खुशहाली", "सोहराय पर्व", "फसल"]
    },
    "motif_younger_brother": {
        "nameHi": "छोटा भाई",
        "nameOlChiki": "ᱦᱩᱰᱤᱧ ᱵᱚᱭᱦᱟ",
        "phoneticDeva": "[हुडिंज बोयहा]",
        "category": "PEOPLE_ACTIONS",
        "grade": 1,
        "tags": ["छोटा भाई", "भाई", "brother", "बोयहा", "परिवार"],
        "contextPrompts": ["हमारा परिवार", "स्नेह", "भाई-बहन"]
    }
}

def main():
    if not os.path.exists(MANIFEST_PATH):
        raise FileNotFoundError(f"Manifest '{MANIFEST_PATH}' not found! Run process_flashcard_assets.py first.")
        
    with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
        manifest = json.load(f)
        
    catalog = []
    missing_lexicon = []
    
    for item in manifest:
        mid = item["id"]
        if mid not in LEXICON:
            missing_lexicon.append(mid)
            continue
            
        lex = LEXICON[mid]
        entry = {
            "id": mid,
            "filename": item["filename"],
            "asset_path": item["asset_path"],
            "name_hi": lex["nameHi"],
            "name_olchiki": lex["nameOlChiki"],
            "phonetic_deva": lex["phoneticDeva"],
            "category": lex["category"],
            "grade": lex["grade"],
            "aspect_ratio": item["aspect_ratio"],
            "compressed_size_kb": item["compressed_size_kb"],
            "tags": lex["tags"],
            "context_prompts": lex["contextPrompts"]
        }
        catalog.append(entry)
        
    if missing_lexicon:
        print(f"WARNING: {len(missing_lexicon)} motifs missing from lexicon:", missing_lexicon)
    else:
        print(f"All {len(catalog)} motifs successfully mapped with authentic bilingual lexicon!")
        
    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(catalog, f, indent=2, ensure_ascii=False)
        
    print(f"Motif catalog written to: {OUTPUT_PATH} ({len(catalog)} items, {os.path.getsize(OUTPUT_PATH)/1024:.2f} KB)")

if __name__ == "__main__":
    main()
