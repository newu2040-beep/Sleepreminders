package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.SleepSession
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SleepReportExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    /**
     * Generates a beautifully aligned plain text (.txt) report.
     */
    fun generateTextReport(context: Context, stats: SleepStats, sessions: List<SleepSession>): File {
        val file = File(context.cacheDir, "Sleep_Cycle_Patterns_Report.txt")
        val sb = StringBuilder()

        sb.appendLine("=============================================")
        sb.appendLine("        SLEEP REMINDERS - COGNITIVE REPORT    ")
        sb.appendLine("=============================================")
        sb.appendLine("Generated on: ${dateFormat.format(Date())}")
        sb.appendLine("User Assessment Profile: Active Rest Optimizer")
        sb.appendLine("=============================================")
        sb.appendLine()
        sb.appendLine("✦ METRICS SUMMARY ✦")
        sb.appendLine("---------------------------------------------")
        sb.appendLine(String.format("Total Sleep Sessions Completed : %d", stats.totalSessions))
        sb.appendLine(String.format("Accumulated Rest Time          : %.1f hrs", stats.totalHoursSlept))
        sb.appendLine(String.format("Average Rest Duration          : %d mins (%d cycles)", stats.averageDurationMinutes, stats.averageDurationMinutes / 90))
        sb.appendLine(String.format("Circadian Consistency Score    : %d%%", stats.consistencyScore))
        sb.appendLine(String.format("Remaining Recovery Sleep Debt  : %.1f hrs", stats.sleepDebtHours))
        sb.appendLine(String.format("Current Undefeated Streak      : %d Days 🔥", stats.streak))
        sb.appendLine("---------------------------------------------")
        sb.appendLine()
        
        sb.appendLine("✦ UNDERSTANDING YOUR PATTERNS ✦")
        sb.appendLine("- Consistent bedtimes secure deep slow-wave stage recovery (non-REM phases 3 & 4).")
        sb.appendLine("- Eliminating your sleep debt of ${String.format("%.1f", stats.sleepDebtHours)} hours decreases overall cognitive fog.")
        sb.appendLine()

        sb.appendLine("✦ EXHAUSTIVE LOGS LISTING ✦")
        sb.appendLine("Date & Time              | Status    | Duration      | Ambient Delay")
        sb.appendLine("-------------------------|-----------|---------------|--------------")
        if (sessions.isEmpty()) {
            sb.appendLine("No logs found. Begin a restful sleep session to record entries.")
        } else {
            sessions.forEach { s ->
                val dateStr = dateFormat.format(Date(s.timestamp)).padEnd(24)
                val statusStr = s.status.padEnd(9)
                val durationStr = "${s.durationMinutes} mins".padEnd(13)
                val delayStr = "${s.delayMinutes} mins delay"
                sb.appendLine("$dateStr | $statusStr | $durationStr | $delayStr")
            }
        }
        sb.appendLine("=============================================")
        sb.appendLine("       Keep scheduling rest. Wake rejuvenated! ")
        sb.appendLine("=============================================")

        FileOutputStream(file).use { out ->
            out.write(sb.toString().toByteArray())
        }
        return file
    }

    /**
     * Generates a playful sleep "Invoice" receipt.
     */
    fun generateInvoiceReport(context: Context, stats: SleepStats, sessions: List<SleepSession>): File {
        val file = File(context.cacheDir, "Sleep_Cycle_Performance_Invoice.txt")
        val sb = StringBuilder()

        val invoiceNumber = "INV-${System.currentTimeMillis() / 10000}"
        val dateStr = dateOnlyFormat.format(Date())

        sb.appendLine("=========================================================================")
        sb.appendLine("                         OFFICIAL REST COGNITIVE INVOICE                 ")
        sb.appendLine("=========================================================================")
        sb.appendLine("INVOICE NO : $invoiceNumber                           DATE: $dateStr")
        sb.appendLine("CLIENT TO  : Active Rest Optimizer Profile")
        sb.appendLine("PROVIDER   : Sleep Reminders & Circadian System Co.")
        sb.appendLine("=========================================================================")
        sb.appendLine()
        sb.appendLine("DESCRIPTION                        | QUANTITY    | QUALITY/RATE | TOTAL IMPACT")
        sb.appendLine("-----------------------------------|-------------|--------------|----------------")
        
        // Item 1: Complete Sessions
        val completedCount = sessions.count { it.status == "COMPLETED" }
        sb.appendLine(
            String.format(
                "%-34s | %-11s | %-12s | %s",
                "1. Restful Cycles Completed (90m ea)",
                "$completedCount sessions",
                "90-min standard",
                "${stats.totalHoursSlept} hrs Rest Credited"
            )
        )

        // Item 2: Consistency
        sb.appendLine(
            String.format(
                "%-34s | %-11s | %-12s | %s",
                "2. Circadian Sync Alignment Credit",
                "${stats.consistencyScore}% score",
                "GABA optimized",
                "${if (stats.consistencyScore >= 80) "EXCELLENT (+$20)" else "MODERATE (+$5)"} mental agility"
            )
        )

        // Item 3: Sleep Debt
        sb.appendLine(
            String.format(
                "%-34s | %-11s | %-12s | %s",
                "3. Outstanding Sleep Debt Balance",
                "${stats.sleepDebtHours} hours",
                "Overdue Rest",
                "DEBT: ${String.format("%.1f hrs", stats.sleepDebtHours)} deficit"
            )
        )

        // Item 4: Current Streak
        sb.appendLine(
            String.format(
                "%-34s | %-11s | %-12s | %s",
                "4. Consecutive Night Loyalty Rewards",
                "${stats.streak} Days",
                "Melatonin boost",
                "STREAK: ${stats.streak} Days Streak 🔥"
            )
        )
        sb.appendLine("-------------------------------------------------------------------------")
        sb.appendLine()

        val netStatus = when {
            stats.sleepDebtHours <= 0 -> "FULLY RECOVERED (Surplus +${Math.abs(stats.sleepDebtHours)}h!)"
            stats.sleepDebtHours <= 5 -> "HEALTHY WINDOW (Minor Debt)"
            else -> "REST OVERDUE DEBT (Urgent Recovery Required)"
        }

        sb.appendLine("SUMMARY STATEMENT CRADLE:")
        sb.appendLine("-------------------------------------------------------------------------")
        sb.appendLine("  [✓] NET PHYSICAL BALANCE    : Peak Cognitive Clarity, Restored Immunity")
        sb.appendLine("  [✓] OVERDUAL SLEEP DEBT FEE : $netStatus")
        sb.appendLine("  [✓] RECOMMENDED ACTION      : Execute power naps or regular full cycles.")
        sb.appendLine("=========================================================================")
        sb.appendLine("         * TERMS: Non-transferrable. Paid in fresh vitality & sharp attention.")
        sb.appendLine("                      THANK YOU FOR SLEEPING WITH US!                     ")
        sb.appendLine("=========================================================================")

        FileOutputStream(file).use { out ->
            out.write(sb.toString().toByteArray())
        }
        return file
    }

    /**
     * Generates a beautifully styled, layout-rich PDF report using Android's native PdfDocument.
     * Generates an A4 page (595 x 842 points) with graphical representations, formatted tables,
     * margins and stylized text.
     */
    fun generatePdfReport(context: Context, stats: SleepStats, sessions: List<SleepSession>): File {
        val file = File(context.cacheDir, "Sleep_Circadian_Performance_Report.pdf")
        val pdfDocument = PdfDocument()

        // Page definition (A4 Size: 595 width, 842 height)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Setup paints
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#1D1B22")
            textSize = 10f
        }

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#4F378B") // Primary Violet Accent
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
        }

        val subtitlePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#4A4458")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 11f
        }

        val sectionPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#334D43") // Sage Green / Tertiary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
        }

        val cardBgPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FAF8FF")
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#E6E1E5")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val headerBgPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#E5DEFF")
            style = Paint.Style.FILL
        }

        var y = 40f

        // Draw Cover Indicator Line
        val topIndicatorPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#4F378B")
            style = Paint.Style.FILL
        }
        canvas.drawRect(30f, y, 565f, y + 6f, topIndicatorPaint)
        y += 28f

        // Draw Header
        canvas.drawText("SLEEP QUALITY & CIRCADIAN REPORT", 30f, y, titlePaint)
        y += 18f
        canvas.drawText("Cognitive Recovery Analysis — Generated on ${dateFormat.format(Date())}", 30f, y, subtitlePaint)
        y += 24f

        // Draw a separator line
        canvas.drawLine(30f, y, 565f, y, borderPaint)
        y += 20f

        // METRICS SECTION HEADER
        canvas.drawText("✦ COGNITIVE RECOVERY KPI METRICS", 30f, y, sectionPaint)
        y += 15f

        // Draw metric card blocks
        // Row 1: Consistency & Sleep Debt
        // Card Left:
        canvas.drawRoundRect(30f, y, 285f, y + 60f, 10f, 10f, cardBgPaint)
        canvas.drawRoundRect(30f, y, 285f, y + 60f, 10f, 10f, borderPaint)
        
        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#4A4458")
            textSize = 9f
        }
        val valuePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#4F378B")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
        }
        val descPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#938F99")
            textSize = 8f
        }

        canvas.drawText("Circadian Consistency", 42f, y + 18f, labelPaint)
        canvas.drawText("${stats.consistencyScore}%", 42f, y + 38f, valuePaint)
        canvas.drawText("Target: >85% for deep non-REM stabilization", 42f, y + 51f, descPaint)

        // Card Right:
        canvas.drawRoundRect(310f, y, 565f, y + 60f, 10f, 10f, cardBgPaint)
        canvas.drawRoundRect(310f, y, 565f, y + 60f, 10f, 10f, borderPaint)

        canvas.drawText("Outstanding Sleep Debt", 322f, y + 18f, labelPaint)
        val debtColor = if (stats.sleepDebtHours > 5) "#B3261E" else "#334D43"
        val debtPaint = Paint(valuePaint).apply {
            color = android.graphics.Color.parseColor(debtColor)
        }
        canvas.drawText(String.format("%.1f hrs", stats.sleepDebtHours), 322f, y + 38f, debtPaint)
        canvas.drawText("Relative to ideal 8.0 hrs baseline", 322f, y + 51f, descPaint)

        y += 75f

        // Row 2: Total Sessions & Streak & Average
        canvas.drawRoundRect(30f, y, 190f, y + 55f, 10f, 10f, cardBgPaint)
        canvas.drawRoundRect(30f, y, 190f, y + 55f, 10f, 10f, borderPaint)
        canvas.drawText("Total Sessions", 40f, y + 16f, labelPaint)
        canvas.drawText("${stats.totalSessions}", 40f, y + 35f, valuePaint)
        canvas.drawText("Completed sleep events", 40f, y + 47f, descPaint)

        canvas.drawRoundRect(205f, y, 385f, y + 55f, 10f, 10f, cardBgPaint)
        canvas.drawRoundRect(205f, y, 385f, y + 55f, 10f, 10f, borderPaint)
        canvas.drawText("Current Streak", 215f, y + 16f, labelPaint)
        canvas.drawText("${stats.streak} Days 🔥", 215f, y + 35f, valuePaint)
        canvas.drawText("Consecutive sleep records", 215f, y + 47f, descPaint)

        canvas.drawRoundRect(400f, y, 565f, y + 55f, 10f, 10f, cardBgPaint)
        canvas.drawRoundRect(400f, y, 565f, y + 55f, 10f, 10f, borderPaint)
        canvas.drawText("Avg Rest Duration", 410f, y + 16f, labelPaint)
        canvas.drawText("${stats.averageDurationMinutes}m", 410f, y + 35f, valuePaint)
        canvas.drawText("${stats.averageDurationMinutes / 90} sleep cycle equivalents", 410f, y + 47f, descPaint)

        y += 75f

        // Playful Sleep Invoice Statement Section in PDF
        canvas.drawText("✦ CIRCADIAN SYSTEM INVOICE BILLING", 30f, y, sectionPaint)
        y += 12f

        canvas.drawRoundRect(30f, y, 565f, y + 75f, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(30f, y, 565f, y + 75f, 8f, 8f, borderPaint)

        val invHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#4F378B")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val invRowPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#1D1B22")
            textSize = 8.5f
        }

        // Table headers
        canvas.drawText("Rest Service Description", 42f, y + 15f, invHeaderPaint)
        canvas.drawText("Rate / Basis", 220f, y + 15f, invHeaderPaint)
        canvas.drawText("Impact Volume", 370f, y + 15f, invHeaderPaint)
        canvas.drawText("Cognitive Balance Due", 470f, y + 15f, invHeaderPaint)

        canvas.drawLine(40f, y + 22f, 555f, y + 22f, borderPaint)

        // Row 1
        canvas.drawText("Completed Sleep Sessions", 42f, y + 34f, invRowPaint)
        canvas.drawText("90-min standard", 220f, y + 34f, invRowPaint)
        canvas.drawText("${sessions.count { it.status == "COMPLETED" }} periods", 370f, y + 34f, invRowPaint)
        canvas.drawText("+${stats.totalHoursSlept} hrs Rest Credited", 470f, y + 34f, invRowPaint)

        // Row 2
        canvas.drawText("Sleep Debt Liability", 42f, y + 46f, invRowPaint)
        canvas.drawText("8 hrs goal base", 220f, y + 46f, invRowPaint)
        canvas.drawText("${stats.sleepDebtHours} hours missing", 370f, y + 46f, invRowPaint)
        canvas.drawText("-${stats.sleepDebtHours}h Overdue Rest", 470f, y + 46f, Paint(invRowPaint).apply { color = android.graphics.Color.parseColor("#B3261E") })

        // Row 3
        canvas.drawText("Circadian System Loyalty Credits", 42f, y + 58f, invRowPaint)
        canvas.drawText("Daily Streak Bonus", 220f, y + 58f, invRowPaint)
        canvas.drawText("${stats.streak} Nights Unbroken", 370f, y + 58f, invRowPaint)
        canvas.drawText("${stats.consistencyScore}% Sync High Alertness", 470f, y + 58f, Paint(invRowPaint).apply { color = android.graphics.Color.parseColor("#334D43") })

        y += 95f

        // RECENT SLEEP RECORDS TABLE
        canvas.drawText("✦ EXHAUSTIVE LOGS RECORDINGS", 30f, y, sectionPaint)
        y += 15f

        // Drawing logs table header
        canvas.drawRect(30f, y, 565f, y + 20f, headerBgPaint)
        canvas.drawRect(30f, y, 565f, y + 20f, borderPaint)

        val colPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#381E72")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
        }

        canvas.drawText("DATE & TIMESTAMP", 38f, y + 13f, colPaint)
        canvas.drawText("STATUS", 200f, y + 13f, colPaint)
        canvas.drawText("DURATION (MINS)", 310f, y + 13f, colPaint)
        canvas.drawText("DELAY PREP", 440f, y + 13f, colPaint)

        y += 20f

        val tableRowPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#1D1B22")
            textSize = 8.5f
        }

        // Limit logs to avoid spilling past PDF page boundary (max 10 logs)
        val logsToPrint = sessions.take(13)
        if (logsToPrint.isEmpty()) {
            canvas.drawRect(30f, y, 565f, y + 30f, cardBgPaint)
            canvas.drawRect(30f, y, 565f, y + 30f, borderPaint)
            canvas.drawText("No completed rest logs on record. Complete sleep schedules to display statistics.", 45f, y + 18f, tableRowPaint)
            y += 30f
        } else {
            logsToPrint.forEachIndexed { i, s ->
                val rowBg = if (i % 2 == 0) "#FAF8FF" else "#FFFFFF"
                val rowBgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor(rowBg)
                }
                canvas.drawRect(30f, y, 565f, y + 22f, rowBgPaint)
                canvas.drawRect(30f, y, 565f, y + 22f, borderPaint)

                val dateFull = dateFormat.format(Date(s.timestamp))
                val stateText = s.status
                val durationText = "${s.durationMinutes} minutes"
                val delayText = "${s.delayMinutes} mins delay"

                canvas.drawText(dateFull, 38f, y + 14f, tableRowPaint)
                val statusColorPaint = Paint(tableRowPaint).apply {
                    color = when (stateText) {
                        "COMPLETED" -> android.graphics.Color.parseColor("#334D43")
                        "CANCELLED" -> android.graphics.Color.parseColor("#B3261E")
                        else -> android.graphics.Color.parseColor("#4A4458")
                    }
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                canvas.drawText(stateText, 200f, y + 14f, statusColorPaint)
                canvas.drawText(durationText, 310f, y + 14f, tableRowPaint)
                canvas.drawText(delayText, 440f, y + 14f, tableRowPaint)

                y += 22f
            }
        }

        // Footer note at bottom of single A4 page
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#938F99")
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("Terms & Conditions: Sleep metrics are self-calculated via local sleep schedule triggers. Circadian synchronization requires consistency.", 30f, 820f, footerPaint)

        pdfDocument.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    /**
     * Easily triggers the ACTION_SEND Intent Chooser to share the file across social media or email.
     */
    fun shareGeneratedReport(context: Context, file: File, mimeType: String, subject: String) {
        val authority = "${context.packageName}.fileprovider"
        val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Here is my official Sleep Cycle & Circadian Performance Report. Rest consistent, wake up rejuvenated! 🌙")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(intent, "Share Sleep Report via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}
