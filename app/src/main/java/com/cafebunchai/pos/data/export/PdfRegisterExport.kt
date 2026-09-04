package com.cafebunchai.pos.data.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.cafebunchai.pos.data.model.Order
import com.cafebunchai.pos.data.model.OrderStatus
import com.cafebunchai.pos.ui.util.formatDateTime
import java.io.OutputStream
import java.time.LocalDate

object PdfRegisterExport {
    fun write(
        out: OutputStream,
        from: LocalDate,
        to: LocalDate,
        orders: List<Order>,
    ) {
        val completed = orders.filter { it.status == OrderStatus.COMPLETED }
        val lines = buildList {
            add("Cafe Bun Chai — Register")
            add("$from  to  $to")
            add(
                "Completed ${completed.size}   Sales Rs ${rupees(completed.sumOf { it.totalPaise })}   " +
                    "Cash ${rupees(completed.filter { it.payment == "cash" }.sumOf { it.totalPaise })}   " +
                    "UPI ${rupees(completed.filter { it.payment == "upi" }.sumOf { it.totalPaise })}",
            )
            add("")
            if (orders.isEmpty()) {
                add("No orders in this range.")
            } else {
                orders.forEach { order ->
                    add(
                        "${formatDateTime(order.createdAt)}  ${order.status}  ${order.type.replace('_', ' ')}  " +
                            "${order.payment}  Rs ${rupees(order.totalPaise)}" +
                            if (order.tableNote.isBlank()) "" else "  ${order.tableNote}",
                    )
                    order.lines.forEach { line ->
                        val extra = if (line.lineNote.isNullOrBlank()) "" else " (${line.lineNote})"
                        add("    ${line.qty} x ${line.nameSnapshot}$extra   Rs ${rupees(line.lineTotalPaise)}")
                    }
                    add("")
                }
            }
        }

        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(44, 24, 16)
            textSize = 16f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(44, 24, 16)
            textSize = 10f
            typeface = Typeface.SANS_SERIF
        }
        val lineHeight = 14f
        val usable = pageHeight - margin * 2
        val linesPerPage = (usable / lineHeight).toInt().coerceAtLeast(1)

        val pdf = PdfDocument()
        var pageNum = 1
        var index = 0
        while (index < lines.size) {
            val page = pdf.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create(),
            )
            val canvas = page.canvas
            var y = margin + 12f
            val chunk = lines.subList(index, (index + linesPerPage).coerceAtMost(lines.size))
            chunk.forEachIndexed { i, text ->
                val paint = if (index == 0 && i == 0) titlePaint else bodyPaint
                canvas.drawText(text.take(95), margin, y, paint)
                y += lineHeight
            }
            pdf.finishPage(page)
            index += linesPerPage
            pageNum++
        }
        if (lines.isEmpty()) {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            page.canvas.drawText("Cafe Bun Chai", margin, margin + 20f, titlePaint)
            pdf.finishPage(page)
        }
        pdf.writeTo(out)
        pdf.close()
    }

    private fun rupees(paise: Int): String = "%.2f".format(paise / 100.0)
}
