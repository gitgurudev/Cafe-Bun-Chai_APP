package com.cafebunchai.pos.data.export

import com.cafebunchai.pos.data.model.Order
import com.cafebunchai.pos.data.model.OrderStatus
import com.cafebunchai.pos.ui.util.formatDateTime
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelRegisterExport {
    fun write(
        out: OutputStream,
        from: LocalDate,
        to: LocalDate,
        orders: List<Order>,
    ) {
        val completed = orders.filter { it.status == OrderStatus.COMPLETED }
        val rows = mutableListOf<List<String>>()
        rows += listOf("Cafe Bun Chai register", "$from to $to", "", "", "", "", "", "", "", "")
        rows += listOf(
            "Completed",
            completed.size.toString(),
            "Sales",
            rupees(completed.sumOf { it.totalPaise }),
            "Cash",
            rupees(completed.filter { it.payment == "cash" }.sumOf { it.totalPaise }),
            "UPI",
            rupees(completed.filter { it.payment == "upi" }.sumOf { it.totalPaise }),
            "",
            "",
        )
        rows += listOf(
            "Date time", "Status", "Type", "Payment", "Table",
            "Item", "Qty", "Unit Rs", "Line Rs", "Order Rs",
        )
        if (orders.isEmpty()) {
            rows += listOf("No orders in this range", "", "", "", "", "", "", "", "", "")
        } else {
            for (order in orders) {
                val lines = order.lines.ifEmpty { null }
                if (lines == null) {
                    rows += orderRow(order, "", "", "", "", true)
                } else {
                    lines.forEachIndexed { index, line ->
                        val extra = if (line.lineNote.isNullOrBlank()) "" else " (${line.lineNote})"
                        rows += orderRow(
                            order,
                            "${line.nameSnapshot}$extra",
                            line.qty.toString(),
                            rupees(line.unitPricePaise),
                            rupees(line.lineTotalPaise),
                            index == 0,
                        )
                    }
                }
            }
        }
        val strings = linkedSetOf<String>()
        rows.forEach { row -> row.forEach { strings.add(it) } }
        val indexOf = strings.withIndex().associate { it.value to it.index }

        ZipOutputStream(out).use { zip ->
            zip.utf8("[Content_Types].xml", contentTypes)
            zip.utf8("_rels/.rels", rels)
            zip.utf8("xl/workbook.xml", workbook)
            zip.utf8("xl/_rels/workbook.xml.rels", workbookRels)
            zip.utf8("xl/styles.xml", styles)
            zip.utf8("xl/sharedStrings.xml", sharedStringsXml(strings.toList()))
            zip.utf8("xl/worksheets/sheet1.xml", sheetXml(rows, indexOf))
        }
    }

    private fun orderRow(
        order: Order,
        item: String,
        qty: String,
        unit: String,
        line: String,
        showTicketTotal: Boolean,
    ) = listOf(
        formatDateTime(order.createdAt),
        order.status,
        order.type.replace('_', ' '),
        order.payment,
        order.tableNote,
        item,
        qty,
        unit,
        line,
        if (showTicketTotal) rupees(order.totalPaise) else "",
    )

    private fun rupees(paise: Int): String = "%.2f".format(paise / 100.0)

    private fun ZipOutputStream.utf8(name: String, body: String) {
        putNextEntry(ZipEntry(name))
        write(body.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun sharedStringsXml(values: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${values.size}" uniqueCount="${values.size}">""")
        values.forEach { v ->
            sb.append("<si><t xml:space=\"preserve\">${esc(v)}</t></si>")
        }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun sheetXml(rows: List<List<String>>, indexOf: Map<String, Int>): String {
        val lastRow = rows.size
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<dimension ref="A1:J$lastRow"/>""")
        sb.append("""<sheetViews><sheetView tabSelected="1" workbookViewId="0"><selection activeCell="A1" sqref="A1"/></sheetView></sheetViews>""")
        sb.append("""<sheetFormatPr defaultRowHeight="15"/>""")
        sb.append("<sheetData>")
        rows.forEachIndexed { r, cols ->
            val rowNum = r + 1
            sb.append("""<row r="$rowNum" spans="1:10">""")
            cols.forEachIndexed { c, value ->
                val ref = cellRef(c, rowNum)
                val si = indexOf[value] ?: 0
                sb.append("""<c r="$ref" t="s"><v>$si</v></c>""")
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun cellRef(col: Int, row: Int): String {
        var n = col + 1
        val letters = StringBuilder()
        while (n > 0) {
            n -= 1
            letters.insert(0, ('A' + n % 26))
            n /= 26
        }
        return "$letters$row"
    }

    private fun esc(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("\u0000", "")

    private const val contentTypes = """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

    private const val rels = """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val workbook = """<?xml version="1.0" encoding="UTF-8"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Register" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private const val workbookRels = """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

    private const val styles = """<?xml version="1.0" encoding="UTF-8"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="1"><fill><patternFill patternType="none"/></fill></fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf/></cellStyleXfs>
<cellXfs count="1"><xf xfId="0"/></cellXfs>
</styleSheet>"""
}
