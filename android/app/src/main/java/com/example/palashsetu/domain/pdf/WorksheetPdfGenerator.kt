package com.example.palashsetu.domain.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Production A4 Vector PDF Worksheet Generator for Vaani-Setu.
 *
 * Generates crisp, printable 72-DPI vector A4 worksheets (595 x 842 points)
 * containing authentic Jharkhand JCERT headers, Ol Chiki & Hindi instructions,
 * cultural realia motifs (Sal leaves, Mahua fruits), and practice rubrics.
 */
object WorksheetPdfGenerator {

    private const val TAG = "WorksheetPdfGenerator"
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842

    fun generateWorksheetPdf(
        context: Context,
        grade: Int = 2,
        isHindi: Boolean = true
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        try {
            renderWorksheet(canvas, grade, isHindi)
            document.finishPage(page)

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()

            val outputFile = File(dir, "VaaniSetu_Worksheet_Grade${grade}.pdf")
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            Log.i(TAG, "Generated worksheet PDF at: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error generating worksheet PDF: ${e.message}", e)
            return null
        } finally {
            document.close()
        }
    }

    private fun renderWorksheet(canvas: Canvas, grade: Int, isHindi: Boolean) {
        // Paints
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val thinBorderPaint = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val subHeaderPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val symbolPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val fillBoxPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // 1. Outer Page Margins & Decorative Double Border
        val margin = 30f
        canvas.drawRect(margin, margin, PAGE_WIDTH - margin, PAGE_HEIGHT - margin, borderPaint)
        canvas.drawRect(margin + 4f, margin + 4f, PAGE_WIDTH - margin - 4f, PAGE_HEIGHT - margin - 4f, thinBorderPaint)

        // 2. Header: Jharkhand State / JCERT Header
        val centerX = PAGE_WIDTH / 2f
        var currentY = margin + 28f

        canvas.drawText("झारखंड शैक्षिक अनुसंधान एवं प्रशिक्षण परिषद (JCERT)", centerX, currentY, headerPaint)
        currentY += 16f
        canvas.drawText("JCERT RANCHI, JHARKHAND • NIPUN BHARAT FLN", centerX, currentY, subHeaderPaint)
        currentY += 18f
        canvas.drawText("बुनियादी साक्षरता एवं संख्याज्ञान (FLN) कार्यपत्रक", centerX, currentY, boldTextPaint.apply { textAlign = Paint.Align.CENTER })
        boldTextPaint.textAlign = Paint.Align.LEFT

        // Divider Line
        currentY += 12f
        canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, borderPaint)

        // 3. Metadata Row (Class, Date, Name, Roll)
        currentY += 18f
        val metaLeft = margin + 15f
        canvas.drawText("कक्षा (Grade): $grade", metaLeft, currentY, boldTextPaint)
        canvas.drawText("विषय: गणित (गणित व संथाली संख्या)", metaLeft + 120f, currentY, boldTextPaint)
        canvas.drawText("दिनांक: ०८/०९/२०२६", PAGE_WIDTH - margin - 140f, currentY, textPaint)

        currentY += 18f
        canvas.drawText("विद्यार्थी का नाम (Student Name): ____________________________________", metaLeft, currentY, textPaint)
        canvas.drawText("क्रमांक (Roll No): ________", PAGE_WIDTH - margin - 150f, currentY, textPaint)

        // Divider Line
        currentY += 12f
        canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, thinBorderPaint)

        // 4. Instruction Box (Vernacular Ol Chiki & Hindi)
        currentY += 14f
        val boxRect = RectF(margin + 12f, currentY, PAGE_WIDTH - margin - 12f, currentY + 68f)
        canvas.drawRoundRect(boxRect, 6f, 6f, fillBoxPaint)
        canvas.drawRoundRect(boxRect, 6f, 6f, thinBorderPaint)

        canvas.drawText("निर्देश / ᱟᱹᱭᱫᱟᱹᱨᱤ (Instructions):", margin + 22f, currentY + 18f, boldTextPaint)
        canvas.drawText("ᱥᱟᱱᱛᱟᱲᱤ: ᱢᱟᱹᱦᱩᱣᱟᱹ ᱡᱚ ᱟᱨ ᱥᱟᱨᱡᱚᱢ ᱥᱟᱠᱟᱢ ᱞᱮᱠᱷᱟᱭ ᱢᱮ ᱟᱨ ᱮᱞᱠᱷᱟ ᱯᱩᱨᱟᱹᱣ ᱢᱮ᱾", margin + 22f, currentY + 36f, boldTextPaint)
        canvas.drawText("हिन्दी: महुआ फल और सखुआ (साल) के पत्तों को गिनें और जोड़कर सही संख्या लिखें।", margin + 22f, currentY + 54f, textPaint)

        currentY += 85f

        // 5. Section 1: Visual Realia Math Exercise (3 Sal leaves + 2 Mahua fruits = 5)
        canvas.drawText("अभ्यास १: गिनो और जोड़ो (Count and Add)", margin + 15f, currentY, boldTextPaint)
        currentY += 15f

        // Draw 3 Boxes: [Leaves Box] + [Fruits Box] = [Result Box]
        val cardWidth = 135f
        val cardHeight = 110f
        val spacing = 20f

        val box1X = margin + 20f
        val box2X = box1X + cardWidth + 35f
        val box3X = box2X + cardWidth + 35f

        // Box 1: Sal Leaves (ᱥᱟᱨᱡᱚᱢ)
        drawExerciseCard(canvas, box1X, currentY, cardWidth, cardHeight, "ᱥᱟᱨᱡᱚᱢ ᱥᱟᱠᱟᱢ", "सखुआ पत्ते: ३", 3, isLeaf = true)

        // Plus Symbol
        canvas.drawText("+", box1X + cardWidth + 17f, currentY + 55f, symbolPaint)

        // Box 2: Mahua Fruits (ᱢᱟᱹᱦᱩᱣᱟᱹ)
        drawExerciseCard(canvas, box2X, currentY, cardWidth, cardHeight, "ᱢᱟᱹᱦᱩᱣᱟᱹ ᱡᱚ", "महुआ फल: २", 2, isLeaf = false)

        // Equals Symbol
        canvas.drawText("=", box2X + cardWidth + 17f, currentY + 55f, symbolPaint)

        // Box 3: Student Answer Box
        val answerRect = RectF(box3X, currentY, box3X + cardWidth, currentY + cardHeight)
        canvas.drawRoundRect(answerRect, 8f, 8f, borderPaint)
        canvas.drawText("ᱡᱚᱛᱚ ᱛᱮ (कुल योग):", box3X + 12f, currentY + 22f, boldTextPaint)

        // Dotted answer line
        val answerBoxInner = RectF(box3X + 25f, currentY + 35f, box3X + cardWidth - 25f, currentY + 85f)
        canvas.drawRect(answerBoxInner, thinBorderPaint)
        canvas.drawText("?", box3X + (cardWidth / 2f), currentY + 68f, symbolPaint)

        currentY += cardHeight + 25f

        // 6. Section 2: Ol Chiki Number Tracing & Writing Rubric
        canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, thinBorderPaint)
        currentY += 18f
        canvas.drawText("अभ्यास २: संथाली अंक लेखन अभ्यास (Ol Chiki Numerals 1 to 5)", margin + 15f, currentY, boldTextPaint)
        currentY += 20f

        val digits = listOf(
            Triple("१", "᱑", "ᱢᱤᱫ (एक)"),
            Triple("२", "᱒", "ᱵᱟᱨ (दो)"),
            Triple("३", "᱓", "ᱯᱮ (तीन)"),
            Triple("४", "᱔", "ᱯᱳᱱ (चार)"),
            Triple("५", "᱕", "ᱢᱚᱬᱮ (पाँच)")
        )

        val digitBoxWidth = (PAGE_WIDTH - (margin * 2) - 40f) / 5f
        for (i in digits.indices) {
            val (deva, olchiki, word) = digits[i]
            val x = margin + 20f + (i * digitBoxWidth)
            val dRect = RectF(x, currentY, x + digitBoxWidth - 8f, currentY + 90f)
            canvas.drawRoundRect(dRect, 4f, 4f, thinBorderPaint)

            canvas.drawText(olchiki, x + (digitBoxWidth / 2f) - 4f, currentY + 38f, symbolPaint)
            canvas.drawText(deva, x + (digitBoxWidth / 2f) - 4f, currentY + 58f, boldTextPaint.apply { textAlign = Paint.Align.CENTER })
            canvas.drawText(word, x + (digitBoxWidth / 2f) - 4f, currentY + 76f, textPaint.apply { textAlign = Paint.Align.CENTER })
            boldTextPaint.textAlign = Paint.Align.LEFT
            textPaint.textAlign = Paint.Align.LEFT
        }

        currentY += 115f

        // 7. Section 3: Word Pairing Exercise (Match the column)
        canvas.drawLine(margin + 10f, currentY, PAGE_WIDTH - margin - 10f, currentY, thinBorderPaint)
        currentY += 18f
        canvas.drawText("अभ्यास ३: सही मिलान करें (Match the column)", margin + 15f, currentY, boldTextPaint)
        currentY += 20f

        val colA = listOf("१. ᱥᱟᱨᱡᱚᱢ (सखुआ)", "२. ᱢᱟᱹᱦᱩᱣᱟᱹ (महुआ)", "३. ᱯᱚᱛᱚᱵ (किताब)", "४. ᱡᱚᱦᱟᱨ (नमस्ते)")
        val colB = listOf("(   ) Greetings / Johar", "(   ) State Tree / Sal Leaf", "(   ) Indigenous Fruit", "(   ) Classroom Book")

        for (j in colA.indices) {
            canvas.drawText(colA[j], margin + 30f, currentY + (j * 20f), textPaint)
            canvas.drawText("○ .............................. ○", centerX - 30f, currentY + (j * 20f), textPaint)
            canvas.drawText(colB[j], centerX + 80f, currentY + (j * 20f), textPaint)
        }

        // 8. Footer Rubric & Signature
        val footerY = PAGE_HEIGHT - margin - 45f
        canvas.drawLine(margin + 10f, footerY, PAGE_WIDTH - margin - 10f, footerY, borderPaint)

        canvas.drawText("मूल्यांकन (Rubric):  [ ] उत्कृष्ट (A)   [ ] संतोषजनक (B)   [ ] पुनरावृत्ति (C)", margin + 15f, footerY + 20f, textPaint)
        canvas.drawText("शिक्षक हस्ताक्षर: ____________________", PAGE_WIDTH - margin - 180f, footerY + 20f, boldTextPaint)

        canvas.drawText("वाणी सेतु (Vaani-Setu) • भाषाई समावेशन मंच • JCERT अनुमोदित शिक्षण सामग्री", centerX, footerY + 36f, subHeaderPaint)
    }

    private fun drawExerciseCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        titleOlChiki: String,
        subTitleHindi: String,
        count: Int,
        isLeaf: Boolean
    ) {
        val rect = RectF(x, y, x + w, y + h)
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val subTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Draw items
        val itemPaint = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val centerX = x + (w / 2f)
        val centerY = y + 40f

        if (isLeaf) {
            // Draw 3 Sal leaves
            for (k in -1..1) {
                val lx = centerX + (k * 30f)
                drawSalLeafVector(canvas, lx, centerY, itemPaint)
            }
        } else {
            // Draw 2 Mahua fruits
            drawMahuaFruitVector(canvas, centerX - 22f, centerY, itemPaint)
            drawMahuaFruitVector(canvas, centerX + 22f, centerY, itemPaint)
        }

        canvas.drawText(titleOlChiki, centerX, y + h - 22f, textPaint)
        canvas.drawText(subTitleHindi, centerX, y + h - 8f, subTextPaint)
    }

    private fun drawSalLeafVector(canvas: Canvas, cx: Float, cy: Float, paint: Paint) {
        val path = Path()
        path.moveTo(cx, cy - 20f)
        path.quadTo(cx + 12f, cy - 5f, cx, cy + 15f)
        path.quadTo(cx - 12f, cy - 5f, cx, cy - 20f)
        canvas.drawPath(path, paint)
        // Center vein
        canvas.drawLine(cx, cy - 20f, cx, cy + 22f, paint)
    }

    private fun drawMahuaFruitVector(canvas: Canvas, cx: Float, cy: Float, paint: Paint) {
        // Fruit body
        canvas.drawCircle(cx, cy, 14f, paint)
        // Stem
        canvas.drawLine(cx, cy - 14f, cx + 4f, cy - 22f, paint)
        // Inner seed outline
        canvas.drawCircle(cx, cy, 5f, paint)
    }

    fun openOrSharePdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "कार्यपत्रक PDF खोलें / प्रिंट करें"))
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open PDF intent: ${e.message}")
        }
    }
}
