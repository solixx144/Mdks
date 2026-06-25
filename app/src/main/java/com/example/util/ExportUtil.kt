package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.BatchScanItemResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtil {

    private const val FILE_PROVIDER_AUTHORITY = "com.aistudio.facesearch.zqyfxw.fileprovider"

    /**
     * Exports the consolidated batch results as a CSV file and opens the sharing intent.
     */
    fun exportBatchToCsv(context: Context, results: List<BatchScanItemResult>) {
        val csvContent = StringBuilder().apply {
            // Header Row
            append("Node Number,Celebrity Name,Similarity Match (%),Estimated Age,Gender,Dominant Emotion,Facial Symmetry (%),Forensic Analysis\n")
            // Data Rows
            results.forEachIndexed { index, item ->
                val cleanName = item.result.celebrityName.replace("\"", "\"\"")
                val cleanGender = item.result.gender.replace("\"", "\"\"")
                val cleanEmotion = item.result.emotion.replace("\"", "\"\"")
                val cleanAnalysis = item.result.analysis.replace("\"", "\"\"")
                append("${index + 1},")
                append("\"$cleanName\",")
                append("${item.result.celebritySimilarity},")
                append("${item.result.age},")
                append("\"$cleanGender\",")
                append("\"$cleanEmotion\",")
                append("${item.result.symmetry},")
                append("\"$cleanAnalysis\"\n")
            }
        }.toString()

        try {
            val file = File(context.cacheDir, "Sherlock_Biometric_Report.csv")
            FileOutputStream(file).use { out ->
                out.write(csvContent.toByteArray())
            }
            shareFile(context, file, "text/csv", "Export CSV Spreadsheet")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Exports the consolidated batch results as a professional PDF report and opens the sharing intent.
     */
    fun exportBatchToPdf(context: Context, results: List<BatchScanItemResult>) {
        try {
            val document = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4 standard (595x842 points)
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            var currentY = 40f
            val marginX = 40f
            val contentWidth = 515f // 595 - 40 - 40

            // Helper to handle pagination dynamically
            fun ensureSpace(neededHeight: Float) {
                if (currentY + neededHeight > 800f) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 40f
                    drawSubPageHeader(canvas)
                }
            }

            // --- DRAW MAIN COVER PAGE HEADER ---
            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#0F172A") // Slate 900
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(marginX, currentY, marginX + contentWidth, currentY + 70f), 8f, 8f, headerBgPaint)

            // Header Title
            val headerTitlePaint = Paint().apply {
                color = Color.parseColor("#00E5FF") // Neon Cyan
                textSize = 14f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText("SHERLOCK BIOMETRIC COGNITIVE REPORT", marginX + 15f, currentY + 28f, headerTitlePaint)

            // Header Subtitle
            val headerSubPaint = Paint().apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            canvas.drawText("CONSOLIDATED BATCH FACIAL INTELLIGENCE DOSSIER", marginX + 15f, currentY + 46f, headerSubPaint)

            // Timestamp
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val datePaint = Paint().apply {
                color = Color.parseColor("#94A3B8") // Slate 400
                textSize = 8f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            canvas.drawText("GENERATED: $dateStr", marginX + 15f, currentY + 58f, datePaint)

            currentY += 85f

            // --- DRAW CONSOLIDATED DASHBOARD PANEL ---
            val panelBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC") // Slate 50
                style = Paint.Style.FILL
            }
            val panelBorderPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0") // Slate 200
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(RectF(marginX, currentY, marginX + contentWidth, currentY + 75f), 6f, 6f, panelBgPaint)
            canvas.drawRoundRect(RectF(marginX, currentY, marginX + contentWidth, currentY + 75f), 6f, 6f, panelBorderPaint)

            val statTitlePaint = Paint().apply {
                color = Color.parseColor("#475569") // Slate 600
                textSize = 8f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val statValuePaint = Paint().apply {
                color = Color.parseColor("#0F172A") // Slate 900
                textSize = 14f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }

            // Dashboard statistics
            val totalScans = results.size
            val avgAge = results.map { it.result.age }.average().toInt()
            val avgSymmetry = results.map { it.result.symmetry }.average().toInt()
            val dominantEmotion = results.map { it.result.emotion }
                .groupBy { it }
                .maxByOrNull { it.value.size }?.key ?: "Neutral"

            val colWidth = contentWidth / 4f
            
            // Col 1: Total Scans
            canvas.drawText("TOTAL NODES", marginX + 15f, currentY + 25f, statTitlePaint)
            canvas.drawText("$totalScans", marginX + 15f, currentY + 45f, statValuePaint)

            // Col 2: Avg Age
            canvas.drawText("AVG AGE", marginX + colWidth + 10f, currentY + 25f, statTitlePaint)
            canvas.drawText("$avgAge YRS", marginX + colWidth + 10f, currentY + 45f, statValuePaint)

            // Col 3: Avg Symmetry
            canvas.drawText("AVG SYMMETRY", marginX + (colWidth * 2) + 10f, currentY + 25f, statTitlePaint)
            canvas.drawText("$avgSymmetry%", marginX + (colWidth * 2) + 10f, currentY + 45f, statValuePaint)

            // Col 4: Dominant Emotion
            val emotionPaint = Paint(statValuePaint).apply {
                color = Color.parseColor("#10B981") // Success green
                textSize = 11f
            }
            canvas.drawText("DOMINANT MOOD", marginX + (colWidth * 3) + 10f, currentY + 25f, statTitlePaint)
            canvas.drawText(dominantEmotion.uppercase(), marginX + (colWidth * 3) + 10f, currentY + 44f, emotionPaint)

            currentY += 95f

            // --- DRAW HEADING FOR INDIVIDUAL NODES ---
            val sectionTitlePaint = Paint().apply {
                color = Color.parseColor("#1E293B") // Slate 800
                textSize = 11f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText("INDIVIDUAL BIOMETRIC PROFILE DETAILS", marginX, currentY, sectionTitlePaint)
            
            val underlinePaint = Paint().apply {
                color = Color.parseColor("#00E5FF") // Neon Cyan
                strokeWidth = 2f
            }
            canvas.drawLine(marginX, currentY + 4f, marginX + 80f, currentY + 4f, underlinePaint)

            currentY += 20f

            // Paints for individual items
            val itemTitlePaint = Paint().apply {
                color = Color.parseColor("#0F172A") // Slate 900
                textSize = 10f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val itemTextPaint = Paint().apply {
                color = Color.parseColor("#334155") // Slate 700
                textSize = 9f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            val itemAnalysisLabelPaint = Paint().apply {
                color = Color.parseColor("#64748B") // Slate 500
                textSize = 8f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val itemAnalysisPaint = TextPaint().apply {
                color = Color.parseColor("#475569") // Slate 600
                textSize = 8.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            val imageBorderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1") // Slate 300
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#F1F5F9") // Slate 100
                strokeWidth = 1f
            }

            // --- DRAW NODE LIST ---
            results.forEachIndexed { index, item ->
                // Let's reserve about 105f - 120f for each node depending on wrapped text height
                val wrappedWidth = contentWidth - 80f // 80f for the thumbnail image + margins

                // Pre-calculate wrapped text height to ensure correct spacing
                val builder = StaticLayout.Builder.obtain(item.result.analysis, 0, item.result.analysis.length, itemAnalysisPaint, wrappedWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.1f)
                    .setIncludePad(false)
                val staticLayout = builder.build()
                
                val nodeRequiredHeight = maxOf(75f, 25f + staticLayout.height) + 15f
                ensureSpace(nodeRequiredHeight)

                // Draw card background for node
                val itemBgPaint = Paint().apply {
                    color = Color.parseColor("#FAFAFA")
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(RectF(marginX, currentY, marginX + contentWidth, currentY + nodeRequiredHeight - 8f), 6f, 6f, itemBgPaint)

                // Draw Node Index Tag
                val indexTagPaint = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 9f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                canvas.drawText("NODE #${index + 1}", marginX + 10f, currentY + 18f, indexTagPaint)

                // Draw Celebrity Match Heading
                val matchNamePaint = Paint().apply {
                    color = Color.parseColor("#00838F") // Teal
                    textSize = 9.5f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                canvas.drawText("LOOKALIKE: ${item.result.celebrityName.uppercase()} (${item.result.celebritySimilarity}% COHENCE)", marginX + 100f, currentY + 18f, matchNamePaint)

                // Scale and Draw Face Thumbnail
                try {
                    val scaledBitmap = Bitmap.createScaledBitmap(item.bitmap, 50, 50, true)
                    canvas.drawBitmap(scaledBitmap, marginX + 10f, currentY + 28f, null)
                    canvas.drawRect(RectF(marginX + 10f, currentY + 28f, marginX + 60f, currentY + 78f), imageBorderPaint)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Draw Metrics List
                val metricsLine1 = "AGE: ${item.result.age} | GENDER: ${item.result.gender.uppercase()}"
                val metricsLine2 = "MOOD: ${item.result.emotion.uppercase()} | SYMMETRY: ${item.result.symmetry}%"
                canvas.drawText(metricsLine1, marginX + 70f, currentY + 34f, itemTextPaint)
                canvas.drawText(metricsLine2, marginX + 70f, currentY + 46f, itemTextPaint)

                // Draw forensic text
                canvas.drawText("FORENSIC BIOMETRIC TELEMETRY ANALYSIS:", marginX + 70f, currentY + 58f, itemAnalysisLabelPaint)
                
                canvas.save()
                canvas.translate(marginX + 70f, currentY + 63f)
                staticLayout.draw(canvas)
                canvas.restore()

                currentY += nodeRequiredHeight
            }

            // Finish last page
            document.finishPage(page)

            // Write to cache file
            val file = File(context.cacheDir, "Sherlock_Biometric_Report.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()
            shareFile(context, file, "application/pdf", "Export PDF Report")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun drawSubPageHeader(canvas: Canvas) {
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#0F172A") // Slate 900
            style = Paint.Style.FILL
        }
        canvas.drawRect(RectF(40f, 20f, 555f, 40f), headerBgPaint)

        val paint = Paint().apply {
            color = Color.parseColor("#00E5FF")
            textSize = 8f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("SHERLOCK BIOMETRIC BATCH SCAN REPORT (CONT.)", 48f, 32f, paint)
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = try {
            FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        } catch (e: Exception) {
            android.net.Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Sherlock Biometric Forensic Summary")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
