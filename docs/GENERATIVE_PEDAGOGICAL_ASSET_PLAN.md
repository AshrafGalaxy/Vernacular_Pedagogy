# Generative Pedagogical Asset Plan

## Purpose

This document specifies the first production set of **150 generated PNG assets** for Grade 1-3 Santhali flashcards and printable workbooks. It is grounded in the 368 validated entries in `data/processed/fln/fln_lexicon.json`; it does not add new lexical translations.

The existing seven photo-derived motifs are prototypes only. They are not the visual direction for this set. The production set will use one coherent, child-readable illustration system generated from prompts, then quality-reviewed by a Santali-speaking educator before it is linked to the lexicon.

## Asset Contract

Every approved asset must have:

- Master: `1024x1024` PNG, transparent background, RGBA.
- Runtime derivative: `400x400` PNG, transparent background, black line art only; target under 45 KB after lossless optimisation.
- One isolated subject or one intentionally defined worksheet scene; no captions, letters, numerals, watermarks, logos, borders, or decorative background.
- Centred composition with at least 10% clear margin on every side; the subject should occupy 70-82% of the canvas.
- A stable filename: `motif_<asset_key>.png`.
- A companion registry entry with `asset_key`, `lexicon_ids`, Hindi source label, Ol Chiki label copied from the locked lexicon, grade, generation prompt, reviewer, and approval state.

Do not ask an image model to render Ol Chiki, Hindi, or numerals. Typesetting belongs in the flashcard/worksheet renderer; image models are unreliable at text.

## Shared Prompt Language

Use this as the prefix for every object, plant, food, and animal prompt:

> **A single [SUBJECT], child-readable educational illustration for a Grade 1-3 Santali classroom workbook, accurate Indian/Santhal-region realia, clean bold black ink contour, a few simple interior contour lines, friendly but not cartoonish proportions, centred, isolated on a pure white background, no text, no letters, no numerals, no border, no shadow, no watermark.**

For people/action scenes, use:

> **A respectful, non-stereotyped Santhal-region child or adult [ACTION/SUBJECT], child-readable primary-school workbook illustration, modest everyday clothing, clear hands and facial expression, clean bold black ink contour, centred with minimal context object only when needed for comprehension, white background, no text, no letters, no numerals, no border, no watermark.**

Negative prompt for every generation:

> **photorealistic, colour fill, gradients, grey wash, busy scene, multiple unrelated objects, text, writing, gibberish letters, numerals, logo, watermark, frame, cropped subject, extra fingers, malformed hands, uncanny face, stereotype, weapon in hand, violence**

## Visual Rules

1. Generate in batches of one concept only. Do not use one prompt to ask for several assets.
2. Generate 4 variants per asset, select one, and retain the selected master plus provenance metadata. The projected review pool is 600 candidate images for 150 final assets.
3. Use a fixed seed/style reference after the first approved batch so animals, people, and objects share line weight and visual language.
4. People must be represented with ordinary, dignified classroom, home, farm, and community contexts. Avoid ceremonial dress unless the lexical item requires it.
5. Dangerous animals and fire are observational, non-threatening scenes. Bow-and-arrow is an object study asset, never a combat scene.
6. For a countable object, generate a single-object asset. Quantity worksheets must repeat the same runtime asset; do not generate separate drawings with different object counts.

## Production Matrix

`Lexicon` lists the validated record ID(s) that justify an asset. `Prompt subject` is substituted into the appropriate shared prompt. The 150 rows are the complete first release.

### A. Food and Fruit: 20 assets

| # | Asset key | Lexicon | Prompt subject | Primary worksheet use |
|---:|---|---|---|---|
| 1 | `mango` | `FLN_VOC_FRU_001` | ripe mango with one leaf | counting, addition |
| 2 | `mahua_fruit` | `FLN_VOC_FRU_002` | cluster of mahua fruits | counting, local flora |
| 3 | `banana` | `FLN_VOC_FRU_003` | small bunch of bananas | counting |
| 4 | `jamun` | `FLN_VOC_FRU_004` | small cluster of jamun berries | sorting |
| 5 | `guava` | `FLN_VOC_FRU_005` | whole guava with one leaf | vocabulary |
| 6 | `jackfruit` | `FLN_VOC_FRU_006` | whole jackfruit | large/small comparison |
| 7 | `papaya` | `FLN_VOC_FRU_007` | whole papaya, one cut half only if seeds are cleanly visible | vocabulary |
| 8 | `ber_fruit` | `FLN_VOC_FRU_008` | two ber fruits on a twig | counting |
| 9 | `tamarind_pod` | `FLN_VOC_FRU_009` | curved tamarind pod with leaf | shape matching |
| 10 | `tendu_fruit` | `FLN_VOC_FRU_010` | tendu fruit on twig | local flora |
| 11 | `water_lota` | `FLN_VOC_FRU_011` | simple metal water lota, no decoration | health routine |
| 12 | `milk_cup` | `FLN_VOC_FRU_012` | plain cup of milk | food vocabulary |
| 13 | `cooked_rice_bowl` | `FLN_VOC_FRU_013` | bowl of cooked rice | food vocabulary |
| 14 | `raw_rice_grains` | `FLN_VOC_FRU_014` | small heap of rice grains | counting, farm-to-food |
| 15 | `salt_bowl` | `FLN_VOC_FRU_015` | small bowl of salt crystals | taste vocabulary |
| 16 | `mustard_oil_bottle` | `FLN_VOC_FRU_016` | plain oil bottle with stopper | food vocabulary |
| 17 | `lentil_bowl` | `FLN_VOC_FRU_017` | bowl of lentils | sorting |
| 18 | `roti` | `FLN_VOC_FRU_018` | one round roti | circle recognition |
| 19 | `jaggery_piece` | `FLN_VOC_FRU_019` | two jaggery chunks | counting |
| 20 | `honeycomb` | `FLN_VOC_FRU_020` | small honeycomb section with one bee nearby | nature vocabulary |

### B. Vegetables: 14 assets

| # | Asset key | Lexicon | Prompt subject | Primary worksheet use |
|---:|---|---|---|---|
| 21 | `potato` | `FLN_VOC_VEG_001` | single potato | counting |
| 22 | `onion` | `FLN_VOC_VEG_002` | onion with roots | vocabulary |
| 23 | `tomato` | `FLN_VOC_VEG_003` | tomato with calyx | colour matching |
| 24 | `eggplant` | `FLN_VOC_VEG_004` | eggplant with calyx | vocabulary |
| 25 | `bottle_gourd` | `FLN_VOC_VEG_005` | bottle gourd | long/short comparison |
| 26 | `pumpkin` | `FLN_VOC_VEG_006` | round yellow-pumpkin form, no colour fill | round shape |
| 27 | `leafy_greens` | `FLN_VOC_VEG_007` | tied bundle of leafy greens | sorting |
| 28 | `green_chilli` | `FLN_VOC_VEG_008` | two green chillies | counting |
| 29 | `ginger` | `FLN_VOC_VEG_009` | ginger rhizome | vocabulary |
| 30 | `garlic` | `FLN_VOC_VEG_010` | garlic bulb | vocabulary |
| 31 | `radish` | `FLN_VOC_VEG_011` | radish with leaves | long/short comparison |
| 32 | `bitter_gourd` | `FLN_VOC_VEG_012` | bitter gourd | texture vocabulary |
| 33 | `cucumber` | `FLN_VOC_VEG_013` | cucumber | long/short comparison |
| 34 | `mushroom` | `FLN_VOC_VEG_014` | one edible mushroom cluster | nature vocabulary |

### C. Animals and Small Fauna: 33 assets

| # | Asset key | Lexicon | Prompt subject | Primary worksheet use |
|---:|---|---|---|---|
| 35 | `cow` | `FLN_VOC_ANI_001` | calm Indian cow, side view | animal vocabulary |
| 36 | `ox` | `FLN_VOC_ANI_002` | working ox, side view | farm vocabulary |
| 37 | `calf` | `FLN_VOC_ANI_003` | calf, side view | family matching |
| 38 | `buffalo` | `FLN_VOC_ANI_004` | water buffalo, side view | animal vocabulary |
| 39 | `goat` | `FLN_VOC_ANI_005` | goat, side view | counting |
| 40 | `sheep` | `FLN_VOC_ANI_006` | sheep, side view | animal vocabulary |
| 41 | `dog` | `FLN_VOC_ANI_007` | village dog, sitting | home vocabulary |
| 42 | `cat` | `FLN_VOC_ANI_008` | cat, sitting | home vocabulary |
| 43 | `horse` | `FLN_VOC_ANI_009` | horse, side view | animal vocabulary |
| 44 | `pig` | `FLN_VOC_ANI_010` | pig, side view | animal vocabulary |
| 45 | `tiger` | `FLN_VOC_ANI_011` | tiger, calm side view, no roar | forest vocabulary |
| 46 | `bear` | `FLN_VOC_ANI_012` | sloth bear, side view | forest vocabulary |
| 47 | `elephant` | `FLN_VOC_ANI_013` | elephant, side view | large/small comparison |
| 48 | `monkey` | `FLN_VOC_ANI_014` | monkey on a simple branch | forest vocabulary |
| 49 | `deer` | `FLN_VOC_ANI_015` | deer, side view | forest vocabulary |
| 50 | `rabbit` | `FLN_VOC_ANI_016` | rabbit, side view | animal vocabulary |
| 51 | `mouse` | `FLN_VOC_ANI_017` | mouse, side view | small/large comparison |
| 52 | `snake` | `FLN_VOC_ANI_018` | non-threatening coiled snake | habitat matching |
| 53 | `frog` | `FLN_VOC_ANI_019` | frog, side view | pond habitat |
| 54 | `fish` | `FLN_VOC_ANI_020` | local freshwater fish, side view | subtraction, pond habitat |
| 55 | `small_bird` | `FLN_VOC_ANI_021` | small bird in flight | sky habitat |
| 56 | `crow` | `FLN_VOC_ANI_022` | crow perched on branch | environment matching |
| 57 | `pigeon` | `FLN_VOC_ANI_023` | pigeon, side view | bird vocabulary |
| 58 | `parrot` | `FLN_VOC_ANI_024` | parrot on branch | colour matching |
| 59 | `peacock` | `FLN_VOC_ANI_025` | peacock, folded tail side view | animal vocabulary |
| 60 | `hen` | `FLN_VOC_ANI_026` | hen, side view | farm vocabulary |
| 61 | `rooster` | `FLN_VOC_ANI_027` | rooster, side view | farm vocabulary |
| 62 | `duck` | `FLN_VOC_ANI_028` | duck, side view | pond habitat |
| 63 | `butterfly` | `FLN_VOC_ANI_029` | butterfly, wings open | symmetry, flower match |
| 64 | `ant` | `FLN_VOC_ANI_030` | ant, enlarged clear profile | counting |
| 65 | `housefly` | `FLN_VOC_ANI_031` | fly, simplified profile | hygiene lesson |
| 66 | `honeybee` | `FLN_VOC_ANI_032` | bee, wings clear | pollination, honey match |
| 67 | `turtle` | `FLN_VOC_ANI_033` | turtle, side view | animal vocabulary |

### D. Classroom Objects: 14 assets

| # | Asset key | Lexicon | Prompt subject | Primary worksheet use |
|---:|---|---|---|---|
| 68 | `book` | `FLN_VOC_OBJ_001` | closed school book, blank cover | object match |
| 69 | `notebook` | `FLN_VOC_OBJ_002` | ruled notebook, blank cover | object match |
| 70 | `pen` | `FLN_VOC_OBJ_003` | simple pen | writing action |
| 71 | `pencil` | `FLN_VOC_OBJ_004` | sharpened pencil | writing action |
| 72 | `slate` | `FLN_VOC_OBJ_005` | blank classroom slate | object match |
| 73 | `chalk` | `FLN_VOC_OBJ_006` | two chalk pieces | counting |
| 74 | `blackboard` | `FLN_VOC_OBJ_007` | blank blackboard on stand | classroom scene |
| 75 | `school_bag` | `FLN_VOC_OBJ_008` | simple school bag | object match |
| 76 | `table` | `FLN_VOC_OBJ_009` | classroom table | furniture match |
| 77 | `chair` | `FLN_VOC_OBJ_010` | simple chair | furniture match |
| 78 | `paper` | `FLN_VOC_OBJ_011` | blank paper sheet | object match |
| 79 | `school_bell` | `FLN_VOC_OBJ_012` | hand bell | sound association |
| 80 | `school_building` | `FLN_VOC_OBJ_013` | small rural school building, no written sign | place match |
| 81 | `classroom` | `FLN_VOC_OBJ_014` | empty classroom with board and benches | place match |

### E. Body Parts: 16 assets

All body-part images are neutral instructional vignettes: no disembodied gore, no medical detail, and no labels.

| # | Asset key | Lexicon | Prompt subject | Primary worksheet use |
|---:|---|---|---|---|
| 82 | `head` | `FLN_VOC_BOD_001` | child head and shoulders, front view | identify body part |
| 83 | `eyes` | `FLN_VOC_BOD_002` | child face with eyes clearly visible | identify body part |
| 84 | `ears` | `FLN_VOC_BOD_003` | child head side view with one ear clear | identify body part |
| 85 | `nose` | `FLN_VOC_BOD_004` | child face, front view with nose clear | identify body part |
| 86 | `mouth` | `FLN_VOC_BOD_005` | child face with closed friendly mouth | identify body part |
| 87 | `teeth` | `FLN_VOC_BOD_006` | child smiling with clean teeth | hygiene sequence |
| 88 | `tongue` | `FLN_VOC_BOD_007` | simple mouth-and-tongue instructional view | taste vocabulary |
| 89 | `hands` | `FLN_VOC_BOD_008` | two open child hands | clap/wash action |
| 90 | `feet` | `FLN_VOC_BOD_009` | two child feet in simple sandals | movement |
| 91 | `stomach` | `FLN_VOC_BOD_010` | child torso outline with hand on belly | body vocabulary |
| 92 | `hair` | `FLN_VOC_BOD_011` | child head with hair and comb | hygiene sequence |
| 93 | `fingernails` | `FLN_VOC_BOD_012` | clean fingernails on one hand | hygiene sequence |
| 94 | `neck` | `FLN_VOC_BOD_013` | child head and neck | identify body part |
| 95 | `finger` | `FLN_VOC_BOD_014` | one hand with index finger raised | counting / body part |
| 96 | `back` | `FLN_VOC_BOD_015` | child back view with simple shirt | identify body part |
| 97 | `chest` | `FLN_VOC_BOD_016` | child torso front view, modest clothing | identify body part |

### F. Counting Cards: 10 assets

These are **quantity scenes**, not numeral glyphs. They support `FLN_VOC_NUM_001` through `FLN_VOC_NUM_010` and Grade 1 counting. Use the approved single `mango` master repeated exactly 1-10 times by a deterministic composition script or a tightly controlled generated layout. The generated/master scene must contain no numerals.

| # | Asset key | Lexicon | Prompt subject | Primary worksheet use |
|---:|---|---|---|---|
| 98 | `count_mango_01` | `FLN_VOC_NUM_001` | one identical mango, evenly spaced | count 1 |
| 99 | `count_mango_02` | `FLN_VOC_NUM_002` | two identical mangoes, evenly spaced | count 2 |
| 100 | `count_mango_03` | `FLN_VOC_NUM_003` | three identical mangoes, evenly spaced | count 3 |
| 101 | `count_mango_04` | `FLN_VOC_NUM_004` | four identical mangoes, two by two | count 4 |
| 102 | `count_mango_05` | `FLN_VOC_NUM_005` | five identical mangoes, clear row | count 5 |
| 103 | `count_mango_06` | `FLN_VOC_NUM_006` | six identical mangoes, two rows | count 6 |
| 104 | `count_mango_07` | `FLN_VOC_NUM_007` | seven identical mangoes, two rows | count 7 |
| 105 | `count_mango_08` | `FLN_VOC_NUM_008` | eight identical mangoes, two rows | count 8 |
| 106 | `count_mango_09` | `FLN_VOC_NUM_009` | nine identical mangoes, three by three | count 9 |
| 107 | `count_mango_10` | `FLN_VOC_NUM_010` | ten identical mangoes, two rows of five | count 10 |

### G. Environment and Realia Scenes: 15 assets

| # | Asset key | Lexicon | Prompt subject | Primary worksheet use |
|---:|---|---|---|---|
| 108 | `mahua_tree` | `FLN_ENV_001` | mature mahua tree | local environment |
| 109 | `clay_pot_water` | `FLN_ENV_003` | clay pot beside a small water cup | object-action match |
| 110 | `bird_in_sky` | `FLN_ENV_007` | one bird flying above a minimal horizon | sky habitat |
| 111 | `river` | `FLN_ENV_008` | gentle river with two banks | environment match |
| 112 | `sal_forest` | `FLN_ENV_009` | sal forest, three clear trunks only | forest habitat |
| 113 | `sunrise` | `FLN_ENV_010` | rising sun over simple horizon | day/night sorting |
| 114 | `moon_night` | `FLN_ENV_011` | moon with two stars | day/night sorting |
| 115 | `rain_cloud` | `FLN_ENV_012` | rain cloud with clear drops | weather match |
| 116 | `butterfly_on_flower` | `FLN_ENV_013` | butterfly resting on one flower | nature sequence |
| 117 | `dry_leaves_pile` | `FLN_ENV_014` | small pile of dry leaves | sorting |
| 118 | `ox_ploughing` | `FLN_ENV_015` | ox pulling simple plough, no person required | farm vocabulary |
| 119 | `fish_pond` | `FLN_ENV_016` | pond with three fish | habitat/counting |
| 120 | `ripe_rice_plant` | `FLN_ENV_020` | ripe rice stalks | farm-to-food |
| 121 | `mountain` | `FLN_ENV_023` | single tall hill or mountain | spatial comparison |
| 122 | `planting_sapling` | `FLN_ENV_025` | child planting a small sapling | environmental care |

### H. Family and Community: 10 assets

These use `kinship_community` records as scene references. Review every people image with a local educator for dignity, clothing appropriateness, and non-stereotyped representation.

| # | Asset key | Lexicon | Prompt subject | Primary worksheet use |
|---:|---|---|---|---|
| 123 | `mother_child` | `FLN_KIN_001` | mother comforting child | family match |
| 124 | `farmer_field` | `FLN_KIN_002`, `FLN_KIN_012` | farmer tending a small field | community roles |
| 125 | `younger_brother` | `FLN_KIN_003` | younger brother with school bag | family match |
| 126 | `older_sister_school` | `FLN_KIN_004` | older sister walking with school bag | family / school |
| 127 | `teacher_class` | `FLN_KIN_005` | teacher pointing to blank board, two seated children | classroom role |
| 128 | `grandparent_story` | `FLN_KIN_006` | grandparent telling story to child | family/community |
| 129 | `two_friends` | `FLN_KIN_008` | two friends standing together | social-emotional match |
| 130 | `doctor_child` | `FLN_KIN_011` | doctor listening to a child with stethoscope | helper role |
| 131 | `girls_playing` | `FLN_KIN_013` | two girls playing a non-competitive hand game | action/community |
| 132 | `children_ball_game` | `FLN_KIN_014` | two children passing a ball | action/community |

### I. Action Vignettes: 10 assets

| # | Asset key | Lexicon | Prompt subject | Primary worksheet use |
|---:|---|---|---|---|
| 133 | `child_sitting` | `FLN_VOC_ACT_001` | child sitting upright on mat | command match |
| 134 | `child_standing` | `FLN_VOC_ACT_002` | child standing upright | command match |
| 135 | `child_reading` | `FLN_VOC_ACT_009` | child reading open blank book | command match |
| 136 | `child_writing` | `FLN_VOC_ACT_010` | child writing on blank paper | command match |
| 137 | `child_listening` | `FLN_VOC_ACT_011` | child listening with hand near ear | command match |
| 138 | `child_speaking` | `FLN_VOC_ACT_012` | child speaking to one peer, no speech bubble | command match |
| 139 | `child_showing_object` | `FLN_VOC_ACT_014` | child showing a book | command match |
| 140 | `child_giving_object` | `FLN_VOC_ACT_015` | child passing a pencil to peer | command match |
| 141 | `child_playing` | `FLN_VOC_ACT_017` | child skipping or playing with a ball | action match |
| 142 | `hand_washing` | `FLN_VOC_ACT_021`, `FLN_BOD_009` | child washing hands with water from lota | hygiene sequence |

### J. Attribute, Shape, and Comparison Assets: 8 assets

These visuals carry the concept; they deliberately reuse approved objects so the curriculum stays coherent.

| # | Asset key | Lexicon | Prompt subject | Primary worksheet use |
|---:|---|---|---|---|
| 143 | `red_mango` | `FLN_VOC_COL_001` | mango with a clear red colour fill for colour-card mode | colour match only |
| 144 | `green_sal_leaf` | `FLN_VOC_COL_002` | sal leaf with a clear green colour fill for colour-card mode | colour match only |
| 145 | `yellow_rice_stalk` | `FLN_VOC_COL_003`, `FLN_ENV_020` | ripe rice stalk with yellow colour fill | colour match only |
| 146 | `big_small_pots` | `FLN_VOC_COL_009`, `FLN_VOC_COL_010` | one large and one small clay pot, clearly separated | size comparison |
| 147 | `long_short_rope` | `FLN_VOC_COL_011`, `FLN_VOC_COL_012` | one long rope and one short rope, parallel | length comparison |
| 148 | `heavy_light_objects` | `FLN_VOC_COL_013`, `FLN_VOC_COL_014` | stone and feather, clearly separated | weight-concept discussion |
| 149 | `circle_shape` | `FLN_VOC_COL_007` | one bold outline circle | shape recognition |
| 150 | `square_shape` | `FLN_VOC_COL_008` | one bold outline square | shape recognition |

## Generation and Review Workflow

1. **Prototype style batch:** Generate the first 12 assets: mango, mahua, banana, cow, goat, fish, book, slate, child reading, mother-child, sal leaf, and clay pot. Approve one visual direction before making the other 138.
2. **Production batches:** Generate by matrix section, beginning with A-D because these yield the most flashcard coverage and are low-risk to review.
3. **Curator check:** Reject outputs with incorrect anatomy, ambiguous crop species, unrecognisable local objects, cultural distortion, text artifacts, or poor silhouette.
4. **Educator check:** A Santali-speaking educator verifies the visual referent against the Hindi/Ol Chiki lexicon record. This is required before a record becomes `approved`.
5. **Technical compile:** Preserve the chosen master in `assets/images/generated_masters/`; derive the print/runtime image into `assets/images/motifs/` using the edge-PNG processor.
6. **Registry update:** Add the selected master, final PNG, seed, prompt version, lexicon references, grade use, reviewer, and date to `assets/images/generated_asset_registry.json`.
7. **Acceptance checks:** Verify transparent alpha, `400x400` dimensions, readable 2 px minimum strokes, no embedded text, no clipped subject, and compressed size under 45 KB.

## Release Sequencing

| Release | Assets | Why |
|---|---:|---|
| R1 | 1-34, 68-81, 98-107 | 58 food, vegetable, classroom, and counting assets enable core Grade 1 flashcards and arithmetic worksheets. |
| R2 | 35-67, 82-97 | 49 animals and body-part assets extend vocabulary, habitat, and health workbooks. |
| R3 | 108-150 | 43 scenes, community, action, and comparison assets enable Grade 2-3 comprehension and contextual worksheets. |

## Out of Scope for This Release

- One image per sentence/command in the full 368-record lexicon. Commands should normally reuse the 150 visual primitives above.
- Any generated text, phonetic guide, Ol Chiki glyph, or worksheet page.
- AI-generated audio or faces based on real identifiable people.
- Artwork that claims to depict a particular Santali community member, ceremony, or sacred practice without verified local reference and approval.

The result is a compact 150-asset vocabulary that covers direct lexical flashcards, counting/repetition, object sorting, food and animal vocabulary, classroom actions, health, community roles, local ecology, and the primary comparison concepts needed for Grade 1-3 workbooks.
