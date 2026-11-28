package com.willykez.campconnect.data.reports

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One column of a report table. [weight] is its share of the page width. */
class PdfColumn(val header: String, val weight: Float)

/** A block of rows under an optional heading (for example one programme). */
class PdfSection(val title: String?, val rows: List<List<String>>)

class PdfSpec(
  val title: String,
  val subtitle: String,
  val campus: String,
  val accent: Int,
  val deep: Int,
  val summary: List<Pair<String, String>>,
  val filters: String,
  val columns: List<PdfColumn>,
  val sections: List<PdfSection>,
  val emptyText: String = "Nothing matches these filters.",
  val pageLabel: String = "Page",
  val ofLabel: String = "of"
)

/**
 * Draws a designed A4 report with Android's own PdfDocument (no extra library): a coloured header band,
 * summary boxes, grouped tables with striped rows, repeating column headers and page numbers.
 */
object PdfReport {
  private const val W = 595
  private const val H = 842
  private const val M = 36f
  private const val FOOT = 46f

  private fun paint(size: Float, color: Int, bold: Boolean = false, italic: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = size
    this.color = color
    typeface = Typeface.create(Typeface.SANS_SERIF, if (bold && italic) Typeface.BOLD_ITALIC else if (bold) Typeface.BOLD else if (italic) Typeface.ITALIC else Typeface.NORMAL)
  }

  private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

  private fun tint(color: Int, alpha: Int) = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

  private fun wrap(text: String, p: Paint, width: Float, maxLines: Int): List<String> {
    val clean = text.replace('\n', ' ').trim()
    if (clean.isEmpty()) return listOf("")
    val lines = ArrayList<String>()
    var cur = ""
    for (word in clean.split(' ').filter { it.isNotEmpty() }) {
      val candidate = if (cur.isEmpty()) word else "$cur $word"
      if (p.measureText(candidate) <= width) {
        cur = candidate
      } else {
        if (cur.isNotEmpty()) lines += cur
        cur = word
        while (p.measureText(cur) > width && cur.length > 1) {
          val n = p.breakText(cur, true, width, null).coerceAtLeast(1)
          if (n >= cur.length) break
          lines += cur.substring(0, n)
          cur = cur.substring(n)
        }
      }
    }
    if (cur.isNotEmpty()) lines += cur
    if (lines.size <= maxLines) return lines
    val kept = lines.take(maxLines).toMutableList()
    var last = kept[maxLines - 1]
    while (last.length > 1 && p.measureText("$last…") > width) last = last.dropLast(1)
    kept[maxLines - 1] = "$last…"
    return kept
  }

  fun render(spec: PdfSpec, out: File) {
    val pages = ArrayList<MutableList<(Canvas) -> Unit>>()
    var ops = ArrayList<(Canvas) -> Unit>()
    pages += ops
    val contentW = W - 2 * M
    val total = spec.columns.sumOf { it.weight.toDouble() }.toFloat()
    val colX = FloatArray(spec.columns.size + 1)
    for (i in spec.columns.indices) colX[i + 1] = colX[i] + contentW * spec.columns[i].weight / total

    val white = Color.WHITE
    val ink = Color.rgb(0x1F, 0x29, 0x37)
    val grey = Color.rgb(0x6B, 0x72, 0x80)
    val bodyPaint = paint(9f, ink)
    val headPaint = paint(9f, white, bold = true)
    val cellPad = 5f

    fun slimHeader(): Float {
      val title = spec.title
      ops += { c ->
        c.drawRect(0f, 0f, W.toFloat(), 34f, fill(spec.accent))
        c.drawText(title, M, 22f, paint(11f, white, bold = true))
        c.drawText("ChuoPulse", W - M - paint(9f, white, bold = true).measureText("ChuoPulse"), 22f, paint(9f, white, bold = true))
      }
      return 34f + 18f
    }

    fun newPage(): Float {
      ops = ArrayList()
      pages += ops
      return slimHeader()
    }

    fun tableHeader(y: Float): Float {
      ops += { c ->
        c.drawRoundRect(RectF(M, y, M + contentW, y + 22f), 4f, 4f, fill(spec.deep))
        spec.columns.forEachIndexed { i, col ->
          c.drawText(wrap(col.header, headPaint, colX[i + 1] - colX[i] - 2 * cellPad, 1)[0], M + colX[i] + cellPad, y + 15f, headPaint)
        }
      }
      return y + 22f
    }

    // ---- First page: band, title, summary ----
    val now = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
    ops += { c ->
      c.drawRect(0f, 0f, W.toFloat(), 118f, fill(spec.deep))
      c.drawRect(0f, 0f, W * 0.62f, 118f, fill(spec.accent))
      c.drawCircle(W - 40f, 20f, 70f, fill(tint(white, 22)))
      c.drawCircle(W - 110f, 118f, 46f, fill(tint(white, 16)))
      c.drawText("ChuoPulse", M, 34f, paint(11f, tint(white, 235), bold = true))
      c.drawText(spec.title, M, 70f, paint(24f, white, bold = true))
      c.drawText(spec.subtitle, M, 90f, paint(11f, tint(white, 225)))
      val right = paint(9f, tint(white, 225))
      c.drawText(spec.campus, W - M - right.measureText(spec.campus), 108f, right)
      c.drawText(now, M, 108f, right)
    }
    var y = 118f + 18f

    if (spec.summary.isNotEmpty()) {
      val n = spec.summary.size.coerceAtMost(4)
      val gap = 8f
      val boxW = (contentW - gap * (n - 1)) / n
      val items = spec.summary.take(n)
      val top = y
      ops += { c ->
        items.forEachIndexed { i, (label, value) ->
          val x = M + i * (boxW + gap)
          c.drawRoundRect(RectF(x, top, x + boxW, top + 52f), 8f, 8f, fill(tint(spec.accent, 28)))
          c.drawRoundRect(RectF(x, top, x + 4f, top + 52f), 2f, 2f, fill(spec.accent))
          c.drawText(value, x + 12f, top + 28f, paint(19f, spec.accent, bold = true))
          c.drawText(wrap(label, paint(8.5f, grey), boxW - 16f, 1)[0], x + 12f, top + 43f, paint(8.5f, grey))
        }
      }
      y += 52f + 12f
    }

    if (spec.filters.isNotBlank()) {
      val lines = wrap(spec.filters, paint(9f, grey, italic = true), contentW, 2)
      val top = y
      ops += { c -> lines.forEachIndexed { i, line -> c.drawText(line, M, top + 9f + i * 12f, paint(9f, grey, italic = true)) } }
      y += lines.size * 12f + 8f
    }

    val rowsTotal = spec.sections.sumOf { it.rows.size }
    if (rowsTotal == 0) {
      val top = y + 20f
      ops += { c -> c.drawText(spec.emptyText, M, top, paint(12f, grey, italic = true)) }
    } else {
      y = tableHeader(y)
      for (section in spec.sections) {
        if (section.rows.isEmpty()) continue
        if (section.title != null) {
          if (y + 24f + 24f > H - FOOT) { y = newPage(); y = tableHeader(y) }
          val top = y
          val title = section.title
          val count = section.rows.size.toString()
          ops += { c ->
            c.drawRect(M, top + 4f, M + contentW, top + 26f, fill(tint(spec.accent, 34)))
            c.drawRect(M, top + 4f, M + 3f, top + 26f, fill(spec.accent))
            val tp = paint(10.5f, spec.accent, bold = true)
            c.drawText(wrap(title, tp, contentW - 60f, 1)[0], M + 10f, top + 19f, tp)
            val cp = paint(9f, spec.accent, bold = true)
            c.drawText(count, M + contentW - 8f - cp.measureText(count), top + 19f, cp)
          }
          y += 28f
        }
        section.rows.forEachIndexed { index, row ->
          val cells = spec.columns.indices.map { i ->
            wrap(row.getOrElse(i) { "" }, bodyPaint, colX[i + 1] - colX[i] - 2 * cellPad, 3)
          }
          val lineCount = cells.maxOf { it.size }
          val rowH = lineCount * 11.5f + 8f
          if (y + rowH > H - FOOT) { y = newPage(); y = tableHeader(y) }
          val top = y
          val zebra = index % 2 == 1
          ops += { c ->
            if (zebra) c.drawRect(M, top, M + contentW, top + rowH, fill(Color.rgb(0xF3, 0xF5, 0xF9)))
            cells.forEachIndexed { i, lines ->
              lines.forEachIndexed { li, line -> c.drawText(line, M + colX[i] + cellPad, top + 13f + li * 11.5f, bodyPaint) }
            }
          }
          y += rowH
        }
        y += 6f
      }
    }

    // ---- Footer on every page, now that the page count is known ----
    val count = pages.size
    val doc = PdfDocument()
    try {
      pages.forEachIndexed { i, list ->
        val page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, i + 1).create())
        val canvas = page.canvas
        canvas.drawColor(white)
        list.forEach { it(canvas) }
        canvas.drawRect(M, H - 34f, W - M, H - 33.4f, fill(Color.rgb(0xD1, 0xD5, 0xDB)))
        val fp = paint(8.5f, grey)
        canvas.drawText("ChuoPulse  ·  ${spec.campus}", M, H - 20f, fp)
        val label = "${spec.pageLabel} ${i + 1} ${spec.ofLabel} $count"
        canvas.drawText(label, W - M - fp.measureText(label), H - 20f, fp)
        doc.finishPage(page)
      }
      out.parentFile?.mkdirs()
      out.outputStream().use { doc.writeTo(it) }
    } finally {
      doc.close()
    }
  }
}

/** Opening, sharing and saving a finished report. */
object ReportFiles {
  fun file(context: Context, name: String): File {
    val safe = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "report" }
    return File(File(context.cacheDir, "exports").apply { mkdirs() }, "$safe.pdf")
  }

  private fun uri(context: Context, file: File) = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

  fun share(context: Context, file: File) {
    val send = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri(context, file))
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  /** Returns false when no PDF viewer is installed. */
  fun open(context: Context, file: File): Boolean {
    val view = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri(context, file), "application/pdf")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching { context.startActivity(view) }.isSuccess
  }

  /** Saves into Downloads/ChuoPulse on Android 10+. On older phones it returns false and the caller shares instead. */
  fun saveToDownloads(context: Context, file: File): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return runCatching {
      val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, file.name)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ChuoPulse")
      }
      val target = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
      context.contentResolver.openOutputStream(target)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
      true
    }.getOrDefault(false)
  }
}
