package com.example.palashsetu.domain.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import com.example.palashsetu.data.local.FlashcardMotifRepository
import com.example.palashsetu.data.model.MotifCategory
import com.example.palashsetu.data.model.MotifItem
import com.example.palashsetu.data.model.NipunCompetency
import com.example.palashsetu.domain.engine.DynamicMotifEngine
import java.io.File
import java.io.FileOutputStream

/**
 * Production Offline Dynamic Worksheet & Multi-Page Student Workbook PDF Generator.
 *
 * Generates crisp, printable 72-DPI vector & bitmap A4 worksheets and multi-page workbooks
 * (595 x 842 points) containing authentic Jharkhand JCERT headers, Ol Chiki & Hindi instructions,
 * cultural realia illustrations (WebP bitmaps with aspect-ratio preservation), and pedagogical rubrics.
 */
object WorksheetPdfGenerator {

    private const val TAG = "WorksheetPdfGenerator"
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842

    private val bitmapCache = mutableMapOf<String, Bitmap>()

    private fun loadAssetBitmap(context: Context, assetPath: String): Bitmap? {
        bitmapCache[assetPath]?.let { return it }
        return try {
            context.assets.open(assetPath).use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) bitmapCache[assetPath] = bmp
                bmp
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading asset bitmap '$assetPath': ${e.message}")
            null
        }
    }

    private fun drawBitmapPreservingAspect(canvas: Canvas, bitmap: Bitmap, targetRect: RectF, paint: Paint = Paint()) {
        val scale = minOf(targetRect.width() / bitmap.width.toFloat(), targetRect.height() / bitmap.height.toFloat())
        val drawW = bitmap.width * scale
        val drawH = bitmap.height * scale
        val left = targetRect.left + (targetRect.width() - drawW) / 2f
        val top = targetRect.top + (targetRect.height() - drawH) / 2f
        val destRect = RectF(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(bitmap, null, destRect, paint)
    }

    /**
     * Generates a synchronized single-page worksheet PDF matching the exact in-app preview.
     */
    fun generateWorksheetPdf(
        context: Context,
        worksheet: com.example.palashsetu.data.model.GeneratedWorksheet
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        try {
            renderSynchronizedWorksheet(context, canvas, worksheet)
            document.finishPage(page)

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()

            val code = worksheet.spec.competency.code
            val actType = worksheet.spec.activityType.name
            val outputFile = File(dir, "VaaniSetu_Worksheet_${code}_${actType}_Grade${worksheet.spec.grade}.pdf")
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            Log.i(TAG, "Generated synchronized worksheet PDF at: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error generating synchronized worksheet PDF: ${e.message}", e)
            return null
        } finally {
            document.close()
        }
    }

    /**
     * Generates a single-page dynamic worksheet PDF (legacy / default spec delegate).
     */
    fun generateWorksheetPdf(
        context: Context,
        grade: Int = 2,
        isHindi: Boolean = true,
        competency: NipunCompetency? = null,
        prompt: String = ""
    ): File? {
        val comp = competency ?: com.example.palashsetu.data.local.NipunCurriculumRepository.getDefaultCompetency(context, grade)
        val spec = com.example.palashsetu.data.model.WorksheetSpec(
            grade = grade,
            competency = comp
        )
        val worksheet = DynamicMotifEngine.generateWorksheet(context, spec)
        return generateWorksheetPdf(context, worksheet)
    }

    /**
     * Generates a comprehensive 4-page printable student workbook (कार्यपुस्तिका).
     */
    fun generateWorkbookPdf(
        context: Context,
        grade: Int = 1,
        isHindi: Boolean = true,
        theme: String = "समग्र बुनियादी साक्षरता एवं संख्याज्ञान"
    ): File? {
        val document = PdfDocument()

        try {
            // Page 1: Cover Page
            val p1Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val p1 = document.startPage(p1Info)
            renderWorkbookCoverPage(context, p1.canvas, grade, isHindi, theme)
            document.finishPage(p1)

            // Page 2: वर्णमाला व चित्र मिलान (Alphabet & Animal/Nature Realia)
            val p2Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
            val p2 = document.startPage(p2Info)
            renderWorkbookPage2AlphabetAnimals(context, p2.canvas, grade, isHindi)
            document.finishPage(p2)

            // Page 3: परिवेश व दैनिक वस्तुएं (Classroom & Body Parts)
            val p3Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create()
            val p3 = document.startPage(p3Info)
            renderWorkbookPage3DailyLife(context, p3.canvas, grade, isHindi)
            document.finishPage(p3)

            // Page 4: संख्याज्ञान व फल गणना (Math & Counting Realia)
            val p4Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 4).create()
            val p4 = document.startPage(p4Info)
            renderWorkbookPage4Numeracy(context, p4.canvas, grade, isHindi)
            document.finishPage(p4)

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()

            val outputFile = File(dir, "VaaniSetu_Workbook_Grade${grade}_FLN.pdf")
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            Log.i(TAG, "Generated 4-page student workbook PDF at: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error generating workbook PDF: ${e.message}", e)
            return null
        } finally {
            document.close()
        }
    }

    // =========================================================================
    // Single-Page Synchronized Worksheet Rendering
    // =========================================================================
    private fun renderSynchronizedWorksheet(
        context: Context,
        canvas: Canvas,
        ws: com.example.palashsetu.data.model.GeneratedWorksheet
    ) {
        val borderPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
        val thinBorderPaint = Paint().apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        val headerPaint = Paint().apply { color = Color.BLACK; textSize = 15f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val subHeaderPaint = Paint().apply { color = Color.DKGRAY; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val boldTextPaint = Paint().apply { color = Color.BLACK; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val symbolPaint = Paint().apply { color = Color.BLACK; textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val fillBoxPaint = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL; isAntiAlias = true }

        // Outer Borders
        val margin = 30f
        canvas.drawRect(margin, margin, PAGE_WIDTH - margin, PAGE_HEIGHT - margin, borderPaint)
        canvas.drawRect(margin + 4f, margin + 4f, PAGE_WIDTH - margin - 4f, PAGE_HEIGHT - margin - 4f, thinBorderPaint)

        // Header: Jharkhand JCERT
        val centerX = PAGE_WIDTH / 2f
        var currentY = margin + 26f
        canvas.drawText("झारखंड शैक्षिक अनुसंधान एवं प्रशिक्षण परिषद (JCERT)", centerX, currentY, headerPaint)
        currentY += 15f
        canvas.drawText("JCERT RANCHI, JHARKHAND • NIPUN BHARAT FLN", centerX, currentY, subHeaderPaint)
        currentY += 16f
        canvas.drawText(ws.titleHi, centerX, currentY, boldTextPaint.apply { textAlign = Paint.Align.CENTER })
        boldTextPaint.textAlign = Paint.Align.LEFT

        currentY += 12f
        canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, borderPaint)

        // Metadata
        currentY += 18f
        val metaLeft = margin + 15f
        val code = ws.spec.competency.code
        val domainLabel = if (ws.spec.competency.isLiteracy) "साक्षरता (Literacy)" else "संख्याज्ञान (Numeracy)"
        canvas.drawText("कक्षा (Grade): ${ws.spec.grade}", metaLeft, currentY, boldTextPaint)
        canvas.drawText("दक्षता: $code ($domainLabel)", metaLeft + 105f, currentY, boldTextPaint)
        canvas.drawText("दिनांक: ${ws.generatedDate}", PAGE_WIDTH - margin - 120f, currentY, textPaint)

        currentY += 18f
        canvas.drawText("विद्यार्थी का नाम (Student Name): ____________________________________", metaLeft, currentY, textPaint)
        canvas.drawText("क्रमांक: ____", PAGE_WIDTH - margin - 120f, currentY, textPaint)

        currentY += 12f
        canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, thinBorderPaint)

        // Instructions Box
        currentY += 14f
        val boxRect = RectF(margin + 12f, currentY, PAGE_WIDTH - margin - 12f, currentY + 54f)
        canvas.drawRoundRect(boxRect, 6f, 6f, fillBoxPaint)
        canvas.drawRoundRect(boxRect, 6f, 6f, thinBorderPaint)

        canvas.drawText("निर्देश / ᱟᱹᱭᱫᱟᱹᱨᱤ (${ws.spec.activityType.labelHi}):", margin + 22f, currentY + 16f, boldTextPaint)
        canvas.drawText("ᱥᱟᱱᱛᱟᱲᱤ: ${ws.instructionOlchiki}", margin + 22f, currentY + 32f, boldTextPaint)
        canvas.drawText("हिन्दी: ${ws.instructionHi}", margin + 22f, currentY + 48f, textPaint)

        currentY += 66f

        when (ws.spec.activityType) {
            com.example.palashsetu.data.model.WorksheetActivityType.MATH_ADDITION -> {
                // Section 1: Math Exercise 1
                if (ws.mathExercise != null) {
                    canvas.drawText("अभ्यास १: गिनो और जोड़ो (Problem 1 - Realia Addition)", margin + 15f, currentY, boldTextPaint)
                    currentY += 14f
                    renderMathRow(context, canvas, margin, currentY, ws.mathExercise, borderPaint, thinBorderPaint, boldTextPaint, symbolPaint)
                    currentY += 105f
                }

                // Section 2: Math Exercise 2
                if (ws.mathExercise2 != null) {
                    canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, thinBorderPaint)
                    currentY += 14f
                    canvas.drawText("अभ्यास २: गिनो और जोड़ो (Problem 2 - Realia Addition)", margin + 15f, currentY, boldTextPaint)
                    currentY += 14f
                    renderMathRow(context, canvas, margin, currentY, ws.mathExercise2, borderPaint, thinBorderPaint, boldTextPaint, symbolPaint)
                    currentY += 105f
                }

                // Section 3: Numeral Tracing
                canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, thinBorderPaint)
                currentY += 14f
                canvas.drawText("अभ्यास ३: संथाली अंक व शब्द लेखन (Ol Chiki Numerals & Tracing)", margin + 15f, currentY, boldTextPaint)
                currentY += 16f
                renderNumeralTracing(canvas, margin, currentY, thinBorderPaint, boldTextPaint, textPaint, symbolPaint)
            }

            com.example.palashsetu.data.model.WorksheetActivityType.MATCH_COLUMN -> {
                // Matching Column
                if (ws.matchingPairs != null && ws.scrambledLabels != null) {
                    canvas.drawText("अभ्यास: चित्र पहचान कर सही संथाली नाम से मिलान करें (Match Picture to Word)", margin + 15f, currentY, boldTextPaint)
                    currentY += 18f
                    renderMatchingColumns(context, canvas, margin, currentY, ws.matchingPairs, ws.scrambledLabels, thinBorderPaint, boldTextPaint, textPaint)
                    currentY += (ws.matchingPairs.size * 56f) + 10f
                }

                // Numeral Tracing below
                canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, thinBorderPaint)
                currentY += 14f
                canvas.drawText("पूरक अभ्यास: संथाली अंक व शब्द (Ol Chiki Numerals)", margin + 15f, currentY, boldTextPaint)
                currentY += 16f
                renderNumeralTracing(canvas, margin, currentY, thinBorderPaint, boldTextPaint, textPaint, symbolPaint)
            }

            com.example.palashsetu.data.model.WorksheetActivityType.VOCAB_TRACING -> {
                // Vocabulary tracing cards
                val items = ws.tracingItems ?: emptyList()
                canvas.drawText("अभ्यास: चित्र पहचान कर शब्द व अक्षर सुंदर लिखें (Word Tracing & Writing)", margin + 15f, currentY, boldTextPaint)
                currentY += 16f

                val rowH = 68f
                for (i in items.indices) {
                    val item = items[i]
                    val rowY = currentY + (i * (rowH + 12f))
                    val rowRect = RectF(margin + 15f, rowY, PAGE_WIDTH - margin - 15f, rowY + rowH)
                    canvas.drawRoundRect(rowRect, 6f, 6f, thinBorderPaint)

                    // Image
                    val imgRect = RectF(rowRect.left + 8f, rowRect.top + 6f, rowRect.left + 70f, rowRect.bottom - 6f)
                    val bmp = loadAssetBitmap(context, item.motif.assetPath)
                    if (bmp != null) {
                        drawBitmapPreservingAspect(canvas, bmp, imgRect)
                    }

                    // Ol Chiki script & phonetics
                    canvas.drawText(item.olchikiWord, rowRect.left + 85f, rowY + 28f, symbolPaint.apply { textAlign = Paint.Align.LEFT })
                    symbolPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText("${item.hindiMeaning} ${item.devaPhonetic}", rowRect.left + 85f, rowY + 50f, boldTextPaint)

                    // Dotted Tracing Box for handwriting
                    val traceRect = RectF(rowRect.right - 180f, rowY + 12f, rowRect.right - 15f, rowY + rowH - 12f)
                    canvas.drawRoundRect(traceRect, 4f, 4f, thinBorderPaint)
                    canvas.drawLine(traceRect.left + 10f, traceRect.centerY(), traceRect.right - 10f, traceRect.centerY(), thinBorderPaint)
                    canvas.drawText("लेखन अभ्यास", traceRect.left + 12f, traceRect.top + 14f, textPaint)
                }
            }

            com.example.palashsetu.data.model.WorksheetActivityType.COMPREHENSIVE_FLN -> {
                // 1. Math Addition
                if (ws.mathExercise != null) {
                    canvas.drawText("अभ्यास १: गिनो और जोड़ो (Realia Count and Add)", margin + 15f, currentY, boldTextPaint)
                    currentY += 14f
                    renderMathRow(context, canvas, margin, currentY, ws.mathExercise, borderPaint, thinBorderPaint, boldTextPaint, symbolPaint)
                    currentY += 105f
                }

                // 2. Numeral Tracing
                canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, thinBorderPaint)
                currentY += 12f
                canvas.drawText("अभ्यास २: संथाली अंक व शब्द लेखन (Ol Chiki Numerals)", margin + 15f, currentY, boldTextPaint)
                currentY += 14f
                renderNumeralTracing(canvas, margin, currentY, thinBorderPaint, boldTextPaint, textPaint, symbolPaint)
                currentY += 88f

                // 3. Match the Column
                if (ws.matchingPairs != null && ws.scrambledLabels != null) {
                    canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, thinBorderPaint)
                    currentY += 12f
                    canvas.drawText("अभ्यास ३: चित्र पहचान कर सही नाम से मिलान करें (Match Picture to Word)", margin + 15f, currentY, boldTextPaint)
                    currentY += 14f
                    renderMatchingColumns(context, canvas, margin, currentY, ws.matchingPairs, ws.scrambledLabels, thinBorderPaint, boldTextPaint, textPaint)
                }
            }
        }

        // Footer Teacher Evaluation
        val footerY = PAGE_HEIGHT - margin - 20f
        canvas.drawLine(margin + 10f, footerY - 10f, PAGE_WIDTH - margin - 10f, footerY - 10f, thinBorderPaint)
        canvas.drawText("शिक्षक हस्ताक्षर: ____________________", margin + 15f, footerY + 5f, textPaint)
        canvas.drawText("ग्रेड/मूल्यांकन: [ A ]  [ B ]  [ C ]", PAGE_WIDTH - margin - 160f, footerY + 5f, boldTextPaint)
    }

    private fun renderMathRow(
        context: Context,
        canvas: Canvas,
        margin: Float,
        currentY: Float,
        mathData: DynamicMotifEngine.MathExerciseData,
        borderPaint: Paint,
        thinBorderPaint: Paint,
        boldTextPaint: Paint,
        symbolPaint: Paint
    ) {
        val cardW = 140f
        val cardH = 90f
        val b1X = margin + 20f
        val b2X = b1X + cardW + 35f
        val b3X = b2X + cardW + 35f

        drawRealiaCard(context, canvas, b1X, currentY, cardW, cardH, mathData.item1, mathData.count1)
        canvas.drawText("+", b1X + cardW + 17f, currentY + 48f, symbolPaint)

        drawRealiaCard(context, canvas, b2X, currentY, cardW, cardH, mathData.item2, mathData.count2)
        canvas.drawText("=", b2X + cardW + 17f, currentY + 48f, symbolPaint)

        val ansRect = RectF(b3X, currentY, b3X + cardW, currentY + cardH)
        canvas.drawRoundRect(ansRect, 6f, 6f, borderPaint)
        canvas.drawText("ᱡᱚᱛᱚ ᱛᱮ (कुल योग):", b3X + 12f, currentY + 22f, boldTextPaint)
        val ansInner = RectF(b3X + 25f, currentY + 32f, b3X + cardW - 25f, currentY + 76f)
        canvas.drawRect(ansInner, thinBorderPaint)
        canvas.drawText("?", b3X + (cardW / 2f), currentY + 62f, symbolPaint)
    }

    private fun renderNumeralTracing(
        canvas: Canvas,
        margin: Float,
        currentY: Float,
        thinBorderPaint: Paint,
        boldTextPaint: Paint,
        textPaint: Paint,
        symbolPaint: Paint
    ) {
        val digits = listOf(
            Triple("१", "᱑", "ᱢᱤᱫ (एक)"),
            Triple("२", "᱒", "ᱵᱟᱨ (दो)"),
            Triple("३", "᱓", "ᱯᱮ (तीन)"),
            Triple("४", "᱔", "ᱯᱳᱱ (चार)"),
            Triple("५", "᱕", "ᱢᱚᱬᱮ (पाँच)")
        )
        val dWidth = (PAGE_WIDTH - (margin * 2) - 40f) / 5f
        for (i in digits.indices) {
            val (deva, olchiki, word) = digits[i]
            val x = margin + 20f + (i * dWidth)
            val dRect = RectF(x, currentY, x + dWidth - 8f, currentY + 72f)
            canvas.drawRoundRect(dRect, 4f, 4f, thinBorderPaint)

            canvas.drawText(olchiki, x + (dWidth / 2f) - 4f, currentY + 28f, symbolPaint)
            canvas.drawText(deva, x + (dWidth / 2f) - 4f, currentY + 46f, boldTextPaint.apply { textAlign = Paint.Align.CENTER })
            canvas.drawText(word, x + (dWidth / 2f) - 4f, currentY + 62f, textPaint.apply { textAlign = Paint.Align.CENTER })
            boldTextPaint.textAlign = Paint.Align.LEFT
            textPaint.textAlign = Paint.Align.LEFT
        }
    }

    private fun renderMatchingColumns(
        context: Context,
        canvas: Canvas,
        margin: Float,
        currentY: Float,
        matchPairs: List<DynamicMotifEngine.MatchingPair>,
        scrambled: List<DynamicMotifEngine.MatchingPair>,
        thinBorderPaint: Paint,
        boldTextPaint: Paint,
        textPaint: Paint
    ) {
        val rowH = 44f
        for (i in matchPairs.indices) {
            val p = matchPairs[i]
            val s = scrambled[i]
            val rowY = currentY + (i * (rowH + 8f))

            // Column A: Picture Card
            val imgRect = RectF(margin + 25f, rowY, margin + 25f + 70f, rowY + rowH)
            canvas.drawRoundRect(imgRect, 4f, 4f, thinBorderPaint)
            val bmp = loadAssetBitmap(context, p.motif.assetPath)
            if (bmp != null) {
                drawBitmapPreservingAspect(canvas, bmp, RectF(imgRect.left + 4f, imgRect.top + 4f, imgRect.right - 4f, imgRect.bottom - 4f))
            }
            // Dot A
            canvas.drawCircle(margin + 115f, rowY + (rowH / 2f), 4f, Paint().apply { color = Color.BLACK })

            // Dot B
            canvas.drawCircle(PAGE_WIDTH - margin - 170f, rowY + (rowH / 2f), 4f, Paint().apply { color = Color.BLACK })

            // Column B: Ol Chiki & Hindi Label
            val labelRect = RectF(PAGE_WIDTH - margin - 155f, rowY, PAGE_WIDTH - margin - 20f, rowY + rowH)
            canvas.drawRoundRect(labelRect, 4f, 4f, thinBorderPaint)
            canvas.drawText(s.motif.nameOlchiki, labelRect.left + 10f, rowY + 18f, boldTextPaint)
            canvas.drawText("${s.motif.nameHi} ${s.motif.phoneticDeva}", labelRect.left + 10f, rowY + 34f, textPaint)
        }
    }

    private fun drawRealiaCard(
        context: Context,
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        item: MotifItem,
        count: Int
    ) {
        val thinBorder = Paint().apply { color = Color.GRAY; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 9f; isAntiAlias = true }
        val boldPaint = Paint().apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        val cardRect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(cardRect, 6f, 6f, thinBorder)

        // Draw Realia Bitmap
        val bmp = loadAssetBitmap(context, item.assetPath)
        val imgRect = RectF(x + 8f, y + 8f, x + w - 8f, y + h - 30f)
        if (bmp != null) {
            drawBitmapPreservingAspect(canvas, bmp, imgRect)
        }

        // Bilingual Label + Count
        canvas.drawText("${item.nameOlchiki} (${count})", x + 10f, y + h - 16f, boldPaint)
        canvas.drawText("${item.nameHi}: $count", x + 10f, y + h - 6f, textPaint)
    }

    // =========================================================================
    // Multi-Page Student Workbook Rendering
    // =========================================================================

    private fun renderWorkbookCoverPage(context: Context, canvas: Canvas, grade: Int, isHindi: Boolean, theme: String) {
        val borderPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2.5f; isAntiAlias = true }
        val thinBorderPaint = Paint().apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        val titlePaint = Paint().apply { color = Color.rgb(0, 35, 111); textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val subTitlePaint = Paint().apply { color = Color.rgb(180, 83, 9); textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val boldPaint = Paint().apply { color = Color.BLACK; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true }

        val margin = 32f
        canvas.drawRect(margin, margin, PAGE_WIDTH - margin, PAGE_HEIGHT - margin, borderPaint)
        canvas.drawRect(margin + 5f, margin + 5f, PAGE_WIDTH - margin - 5f, PAGE_HEIGHT - margin - 5f, thinBorderPaint)

        var y = margin + 50f
        val cx = PAGE_WIDTH / 2f

        canvas.drawText("झारखंड शैक्षिक अनुसंधान एवं प्रशिक्षण परिषद (JCERT)", cx, y, boldPaint.apply { textAlign = Paint.Align.CENTER; textSize = 13f })
        y += 18f
        canvas.drawText("JCERT RANCHI, JHARKHAND", cx, y, textPaint.apply { textAlign = Paint.Align.CENTER; textSize = 10f })
        y += 40f

        canvas.drawText("बुनियादी साक्षरता एवं संख्याज्ञान (FLN)", cx, y, titlePaint)
        y += 24f
        canvas.drawText("ᱥᱟᱱᱛᱟᱲᱤ ᱟᱨ ᱦᱤᱱᱫᱤ ᱯᱟᱹᱴᱷᱩᱣᱟᱹ ᱠᱟᱹᱢᱤ ᱯᱩᱛᱷᱤ", cx, y, subTitlePaint)
        y += 18f
        canvas.drawText("संथाली एवं हिन्दी छात्र अभ्यास कार्यपुस्तिका (Grade $grade)", cx, y, boldPaint.apply { textAlign = Paint.Align.CENTER; textSize = 12f })

        // Hero Motif Illustration: Classroom / Tree
        y += 30f
        val heroMotif = FlashcardMotifRepository.getMotifById(context, "motif_school_building")
            ?: FlashcardMotifRepository.getMotifById(context, "motif_mahua_tree")
        if (heroMotif != null) {
            val bmp = loadAssetBitmap(context, heroMotif.assetPath)
            if (bmp != null) {
                val heroRect = RectF(cx - 150f, y, cx + 150f, y + 170f)
                drawBitmapPreservingAspect(canvas, bmp, heroRect)
                y += 185f
            }
        } else {
            y += 140f
        }

        // Student Information Card
        val formRect = RectF(margin + 30f, y, PAGE_WIDTH - margin - 30f, y + 150f)
        canvas.drawRoundRect(formRect, 8f, 8f, thinBorderPaint)

        boldPaint.textAlign = Paint.Align.LEFT
        boldPaint.textSize = 11f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 11f

        var fy = formRect.top + 28f
        val fx = formRect.left + 20f
        canvas.drawText("विद्यार्थी का नाम (Student Name): ____________________________________", fx, fy, textPaint)
        fy += 26f
        canvas.drawText("कक्षा (Grade): $grade          क्रमांक (Roll No): ____________", fx, fy, textPaint)
        fy += 26f
        canvas.drawText("विद्यालय का नाम (School): __________________________________________", fx, fy, textPaint)
        fy += 26f
        canvas.drawText("प्रखंड व जिला (Block & District): ____________________________________", fx, fy, textPaint)

        // Footer Note
        val footY = PAGE_HEIGHT - margin - 30f
        canvas.drawText("राष्ट्रीय शिक्षा नीति (NEP 2020) • निपुण भारत मिशन • वाणी-सेतु डिजिटल नवाचार", cx, footY, boldPaint.apply { textAlign = Paint.Align.CENTER; textSize = 9f })
    }

    private fun renderWorkbookPage2AlphabetAnimals(context: Context, canvas: Canvas, grade: Int, isHindi: Boolean) {
        val borderPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
        val thinBorder = Paint().apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        val headerPaint = Paint().apply { color = Color.BLACK; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val boldPaint = Paint().apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 9f; isAntiAlias = true }

        val margin = 30f
        canvas.drawRect(margin, margin, PAGE_WIDTH - margin, PAGE_HEIGHT - margin, borderPaint)

        var y = margin + 25f
        canvas.drawText("पाठ १: वन्य जीव व पशु-पक्षी (Animals & Birds Realia)", margin + 15f, y, headerPaint)
        y += 14f
        canvas.drawText("ᱪᱤᱛᱟᱹᱨ ᱧᱮᱞ ᱠᱟᱛᱮ ᱧᱩᱛᱩᱢ ᱯᱟᱲᱦᱟᱣ ᱢᱮ ᱟᱨ ᱞᱟᱛᱟᱨ ᱨᱮ ᱚᱞ ᱢᱮ᱾ (चित्र देखकर नाम पढ़ें व लिखें)", margin + 15f, y, textPaint)
        y += 20f

        val animals = FlashcardMotifRepository.filterMotifs(context, category = MotifCategory.ANIMALS, grade = 1).take(6)
        val gridCols = 2
        val colW = (PAGE_WIDTH - (margin * 2) - 30f) / 2f
        val cardH = 200f

        for (i in animals.indices) {
            val item = animals[i]
            val row = i / gridCols
            val col = i % gridCols
            val cx = margin + 10f + (col * (colW + 10f))
            val cy = y + (row * (cardH + 12f))

            val cRect = RectF(cx, cy, cx + colW, cy + cardH)
            canvas.drawRoundRect(cRect, 6f, 6f, thinBorder)

            // Bitmap
            val bmp = loadAssetBitmap(context, item.assetPath)
            if (bmp != null) {
                val bRect = RectF(cx + 8f, cy + 8f, cx + colW - 8f, cy + cardH - 70f)
                drawBitmapPreservingAspect(canvas, bmp, bRect)
            }

            // Labels
            val ly = cy + cardH - 52f
            canvas.drawText(item.nameOlchiki, cx + 12f, ly, boldPaint.apply { textSize = 14f })
            canvas.drawText("${item.nameHi}  ${item.phoneticDeva}", cx + 12f, ly + 18f, textPaint.apply { textSize = 10f })

            // Tracing Guide Line
            canvas.drawLine(cx + 12f, cy + cardH - 12f, cx + colW - 12f, cy + cardH - 12f, thinBorder)
        }

        // Page Number
        canvas.drawText("पृष्ठ २ (Page 2)", PAGE_WIDTH / 2f, PAGE_HEIGHT - margin - 12f, textPaint.apply { textAlign = Paint.Align.CENTER })
    }

    private fun renderWorkbookPage3DailyLife(context: Context, canvas: Canvas, grade: Int, isHindi: Boolean) {
        val borderPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
        val thinBorder = Paint().apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        val headerPaint = Paint().apply { color = Color.BLACK; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val boldPaint = Paint().apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 9f; isAntiAlias = true }

        val margin = 30f
        canvas.drawRect(margin, margin, PAGE_WIDTH - margin, PAGE_HEIGHT - margin, borderPaint)

        var y = margin + 25f
        canvas.drawText("पाठ २: हमारा शरीर व दैनिक क्रियाएं (Body Parts & Healthy Habits)", margin + 15f, y, headerPaint)
        y += 14f
        canvas.drawText("ᱦᱚᱲᱢᱚ ᱨᱮᱭᱟᱜ ᱦᱟᱹᱴᱤᱧ ᱩᱨᱩᱢ ᱢᱮ ᱟᱨ ᱥᱟᱯᱷᱟ-ᱥᱟᱹᱯᱷᱤ ᱪᱮᱫ ᱢᱮ᱾ (शरीर के अंग पहचानें व स्वच्छता सीखें)", margin + 15f, y, textPaint)
        y += 20f

        val bodyItems = FlashcardMotifRepository.filterMotifs(context, category = MotifCategory.BODY_PARTS).take(4)
        val habits = FlashcardMotifRepository.filterMotifs(context, category = MotifCategory.PEOPLE_ACTIONS).take(2)
        val combined = bodyItems + habits

        val gridCols = 2
        val colW = (PAGE_WIDTH - (margin * 2) - 30f) / 2f
        val cardH = 200f

        for (i in combined.indices) {
            val item = combined[i]
            val row = i / gridCols
            val col = i % gridCols
            val cx = margin + 10f + (col * (colW + 10f))
            val cy = y + (row * (cardH + 12f))

            val cRect = RectF(cx, cy, cx + colW, cy + cardH)
            canvas.drawRoundRect(cRect, 6f, 6f, thinBorder)

            val bmp = loadAssetBitmap(context, item.assetPath)
            if (bmp != null) {
                val bRect = RectF(cx + 8f, cy + 8f, cx + colW - 8f, cy + cardH - 70f)
                drawBitmapPreservingAspect(canvas, bmp, bRect)
            }

            val ly = cy + cardH - 52f
            canvas.drawText(item.nameOlchiki, cx + 12f, ly, boldPaint.apply { textSize = 14f })
            canvas.drawText("${item.nameHi}  ${item.phoneticDeva}", cx + 12f, ly + 18f, textPaint.apply { textSize = 10f })
            canvas.drawLine(cx + 12f, cy + cardH - 12f, cx + colW - 12f, cy + cardH - 12f, thinBorder)
        }

        canvas.drawText("पृष्ठ ३ (Page 3)", PAGE_WIDTH / 2f, PAGE_HEIGHT - margin - 12f, textPaint.apply { textAlign = Paint.Align.CENTER })
    }

    private fun renderWorkbookPage4Numeracy(context: Context, canvas: Canvas, grade: Int, isHindi: Boolean) {
        val borderPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
        val thinBorder = Paint().apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        val headerPaint = Paint().apply { color = Color.BLACK; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val boldPaint = Paint().apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 9f; isAntiAlias = true }
        val symbolPaint = Paint().apply { color = Color.BLACK; textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true }

        val margin = 30f
        canvas.drawRect(margin, margin, PAGE_WIDTH - margin, PAGE_HEIGHT - margin, borderPaint)

        var y = margin + 25f
        canvas.drawText("पाठ ३: संख्याज्ञान व फल गणना (Counting Realia & Addition 1 to 10)", margin + 15f, y, headerPaint)
        y += 14f
        canvas.drawText("ᱩᱞ ᱟᱨ ᱡᱚ ᱞᱮᱠᱷᱟᱭ ᱢᱮ ᱟᱨ ᱮᱞᱠᱷᱟ ᱯᱩᱨᱟᱹᱣ ᱢᱮ᱾ (आम और फलों को गिनें और जोड़ पूरा करें)", margin + 15f, y, textPaint)
        y += 22f

        // 3 Realia Addition Exercises
        val exercises = listOf(
            Triple(2, 3, 5),
            Triple(3, 4, 7),
            Triple(4, 5, 9)
        )

        val mangoItem = FlashcardMotifRepository.getMotifById(context, "motif_mango")
            ?: FlashcardMotifRepository.getMotifs(context).first()
        val bananaItem = FlashcardMotifRepository.getMotifById(context, "motif_banana")
            ?: FlashcardMotifRepository.getMotifs(context).last()

        val exCardH = 175f
        for (i in exercises.indices) {
            val (c1, c2, tot) = exercises[i]
            val ey = y + (i * (exCardH + 15f))
            val eRect = RectF(margin + 15f, ey, PAGE_WIDTH - margin - 15f, ey + exCardH)
            canvas.drawRoundRect(eRect, 6f, 6f, thinBorder)

            canvas.drawText("सवाल ${i + 1}:", eRect.left + 15f, ey + 22f, boldPaint)

            // Box 1
            val b1 = RectF(eRect.left + 25f, ey + 35f, eRect.left + 155f, ey + exCardH - 25f)
            canvas.drawRoundRect(b1, 4f, 4f, thinBorder)
            val bmp1 = loadAssetBitmap(context, mangoItem.assetPath)
            if (bmp1 != null) drawBitmapPreservingAspect(canvas, bmp1, RectF(b1.left + 5f, b1.top + 5f, b1.right - 5f, b1.bottom - 22f))
            canvas.drawText("${mangoItem.nameOlchiki} : $c1", b1.left + 10f, b1.bottom - 8f, boldPaint)

            // Plus
            canvas.drawText("+", eRect.left + 175f, ey + 85f, symbolPaint)

            // Box 2
            val b2 = RectF(eRect.left + 195f, ey + 35f, eRect.left + 325f, ey + exCardH - 25f)
            canvas.drawRoundRect(b2, 4f, 4f, thinBorder)
            val bmp2 = loadAssetBitmap(context, bananaItem.assetPath)
            if (bmp2 != null) drawBitmapPreservingAspect(canvas, bmp2, RectF(b2.left + 5f, b2.top + 5f, b2.right - 5f, b2.bottom - 22f))
            canvas.drawText("${bananaItem.nameOlchiki} : $c2", b2.left + 10f, b2.bottom - 8f, boldPaint)

            // Equals
            canvas.drawText("=", eRect.left + 345f, ey + 85f, symbolPaint)

            // Student Answer Box
            val b3 = RectF(eRect.left + 365f, ey + 45f, eRect.left + 465f, ey + exCardH - 35f)
            canvas.drawRect(b3, borderPaint)
            canvas.drawText("?", b3.centerX(), b3.centerY() + 8f, symbolPaint)
        }

        // Footer evaluation
        val footerY = PAGE_HEIGHT - margin - 20f
        canvas.drawText("विद्यार्थी प्रगति मूल्यांकन: [ उत्कृष्ट ]  [ संतोषजनक ]  [ सुधार अपेक्षित ]", margin + 25f, footerY + 5f, boldPaint)
        canvas.drawText("पृष्ठ ४ (Page 4)", PAGE_WIDTH / 2f, footerY + 14f, textPaint.apply { textAlign = Paint.Align.CENTER })
    }
}
