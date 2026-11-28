package com.willykez.campconnect.data.docs

import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads a timetable written as rows of: Day, Time, Course Code, Course Name, Venue.
 * It works on the plain text of a CSV, Excel, Word or PDF file (cells split by | tab , ; or two spaces), and also on a
 * single line like "Friday 08:00-10:00 CS210 Algorithms LAB-1". A day may also stand alone on its own line above its rows.
 *
 * The result has the same shape the AI assistant returns, so the review screen, the clash checks and the
 * "apply" step are shared. When the file does not look like this format it returns null and the AI is used instead.
 */
object TimetableTableParser {
  private val DAYS = mapOf(
    "monday" to 1, "mon" to 1, "tuesday" to 2, "tue" to 2, "tues" to 2, "wednesday" to 3, "wed" to 3,
    "thursday" to 4, "thu" to 4, "thur" to 4, "thurs" to 4, "friday" to 5, "fri" to 5, "saturday" to 6, "sat" to 6,
    "sunday" to 7, "sun" to 7,
    "jumatatu" to 1, "jumanne" to 2, "jumatano" to 3, "alhamisi" to 4, "ijumaa" to 5, "jumamosi" to 6, "jumapili" to 7
  )
  private val RANGE = Regex("""^(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm)?\s*(?:-|–|—|to|hadi)\s*(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm)?$""", RegexOption.IGNORE_CASE)
  private val SINGLE_TIME = Regex("""^(\d{1,2})[:.](\d{2})\s*(am|pm)?$""", RegexOption.IGNORE_CASE)
  private val CODE = Regex("""^[A-Za-z]{2,6}[\s-]?\d{2,4}[A-Za-z]?$""")
  private val FREE_LINE = Regex(
    """^(\w+)\s*[,:\-]?\s+(\d{1,2}(?:[:.]\d{2})?\s*(?:am|pm)?\s*(?:-|–|—|to)\s*\d{1,2}(?:[:.]\d{2})?\s*(?:am|pm)?)\s+([A-Za-z]{2,6}[\s-]?\d{2,4}[A-Za-z]?)\s+(.+)$""",
    RegexOption.IGNORE_CASE
  )
  private val STOP = setOf("of", "in", "and", "the", "for", "bsc", "ba", "bed", "beng", "msc", "bachelor", "diploma", "certificate", "degree", "with", "honours")

  private class Row(val day: Int?, val start: String, val end: String, val code: String, val name: String, val venue: String)

  private fun dayOf(cell: String): Int? = DAYS[cell.trim().trimEnd(':', '.', ',').lowercase()]

  private fun csvSplit(line: String, sep: Char): List<String> {
    val out = ArrayList<String>()
    val cur = StringBuilder()
    var quoted = false
    for (ch in line) {
      when {
        ch == '"' -> quoted = !quoted
        ch == sep && !quoted -> { out += cur.toString(); cur.setLength(0) }
        else -> cur.append(ch)
      }
    }
    out += cur.toString()
    return out
  }

  private fun split(line: String): List<String> {
    val l = line.trim()
    val raw = when {
      '|' in l -> l.split('|')
      '\t' in l -> l.split('\t')
      l.count { it == ',' } >= 3 -> csvSplit(l, ',')
      l.count { it == ';' } >= 3 -> l.split(';')
      else -> l.split(Regex("""\s{2,}"""))
    }
    return raw.map { it.trim().trim('"').trim() }.filter { it.isNotEmpty() }
  }

  private fun to24(h: Int, ap: String?): Int = when (ap?.lowercase()) {
    "pm" -> if (h == 12) 12 else h + 12
    "am" -> if (h == 12) 0 else h
    else -> h
  }

  private fun fmt(h: Int, m: Int) = "%02d:%02d".format(h, m)

  private fun range(h1: Int, m1: Int, ap1: String?, h2: Int, m2: Int, ap2: String?): Pair<String, String>? {
    if (h1 > 24 || h2 > 24 || m1 > 59 || m2 > 59) return null
    var s: Int
    var e: Int
    if (ap1.isNullOrBlank() && ap2.isNullOrBlank()) {
      // No am/pm written: 1 to 6 means the afternoon.
      s = if (h1 in 1..6) h1 + 12 else h1
      e = if (h2 in 1..6) h2 + 12 else h2
      if (e <= s && e < 12) e += 12
    } else {
      val first = if (ap1.isNullOrBlank()) ap2 else ap1
      s = to24(h1, first)
      e = to24(h2, ap2 ?: first)
      if (ap1.isNullOrBlank() && s >= e && s >= 12) s -= 12
    }
    if (s > 23 || e > 23 || e * 60 + m2 <= s * 60 + m1) return null
    return fmt(s, m1) to fmt(e, m2)
  }

  private fun timeFrom(cell: String): Pair<String, String>? {
    val m = RANGE.matchEntire(cell.trim()) ?: return null
    val g = m.groupValues
    return range(g[1].toInt(), g[2].ifEmpty { "0" }.toInt(), g[3].ifEmpty { null }, g[4].toInt(), g[5].ifEmpty { "0" }.toInt(), g[6].ifEmpty { null })
  }

  private fun parseCells(cells: List<String>, currentDay: Int?): Row? {
    var dayIdx = -1
    var timeIdx = -1
    var timeIdx2 = -1
    var time: Pair<String, String>? = null
    var day: Int? = null
    for ((i, c) in cells.withIndex()) {
      if (dayIdx < 0) dayOf(c)?.let { dayIdx = i; day = it; return@let }
      if (time == null) timeFrom(c)?.let { time = it; timeIdx = i }
    }
    if (time == null) {
      // Start and end in two separate cells.
      for (i in 0 until cells.size - 1) {
        val a = SINGLE_TIME.matchEntire(cells[i])
        val b = SINGLE_TIME.matchEntire(cells[i + 1])
        if (a != null && b != null) {
          time = range(a.groupValues[1].toInt(), a.groupValues[2].toInt(), a.groupValues[3].ifEmpty { null }, b.groupValues[1].toInt(), b.groupValues[2].toInt(), b.groupValues[3].ifEmpty { null })
          if (time != null) { timeIdx = i; timeIdx2 = i + 1; break }
        }
      }
    }
    val t = time ?: return null
    val codeIdx = cells.indices.firstOrNull { it != dayIdx && it != timeIdx && it != timeIdx2 && CODE.matches(cells[it]) } ?: return null
    val rest = cells.filterIndexed { i, _ -> i != dayIdx && i != timeIdx && i != timeIdx2 && i != codeIdx }
    val name = when {
      rest.isEmpty() -> ""
      rest.size == 1 -> rest[0]
      else -> rest.dropLast(1).joinToString(" ")
    }
    val venue = if (rest.size >= 2) rest.last() else ""
    return Row(day ?: currentDay, t.first, t.second, cells[codeIdx], name, venue)
  }

  private fun parseFree(line: String, currentDay: Int?): Row? {
    val m = FREE_LINE.matchEntire(line.trim()) ?: return null
    val day = dayOf(m.groupValues[1]) ?: currentDay
    val time = timeFrom(m.groupValues[2].replace(Regex("""\s+"""), " ").replace(Regex("""\s*(-|–|—)\s*"""), "-")) ?: return null
    val tail = m.groupValues[4].trim()
    val parts = tail.split(Regex("""\s+[-–@]\s+|\s{2,}""")).map { it.trim() }.filter { it.isNotEmpty() }
    val name: String
    val venue: String
    if (parts.size >= 2) { name = parts.dropLast(1).joinToString(" "); venue = parts.last() }
    else {
      val words = tail.split(' ')
      if (words.size >= 2) { name = words.dropLast(1).joinToString(" "); venue = words.last() } else { name = tail; venue = "" }
    }
    return Row(day, time.first, time.second, m.groupValues[3], name, venue)
  }

  private fun normal(code: String) = code.uppercase().replace(Regex("""[\s-]"""), "")
  private fun prefixOf(code: String) = code.takeWhile { it.isLetter() }.uppercase()

  private fun fromHeading(text: String, programmes: Collection<String>): String? {
    val h = text.lowercase()
    return programmes.filter { it.isNotBlank() && h.contains(it.lowercase()) }.maxByOrNull { it.length }
  }

  private fun byPrefix(code: String, programmeCourses: Map<String, Set<String>>): String? {
    val prefix = prefixOf(code)
    if (prefix.length < 2) return null
    val scores = programmeCourses.mapValues { (_, codes) -> codes.count { prefixOf(it) == prefix } }.filterValues { it > 0 }
    val best = scores.maxByOrNull { it.value } ?: return null
    return if (scores.count { it.value == best.value } > 1) null else best.key
  }

  private fun byInitials(code: String, programmes: Collection<String>): String? {
    val prefix = prefixOf(code)
    if (prefix.length < 2) return null
    return programmes.filter { name ->
      val initials = name.split(Regex("[^A-Za-z]+")).filter { it.isNotEmpty() && it.lowercase() !in STOP }.joinToString("") { it.first().uppercase() }
      initials.length >= 2 && (initials == prefix || prefix.startsWith(initials))
    }.singleOrNull()
  }

  /** Returns the proposal in the assistant's JSON shape, or null when the text is not in this format. */
  fun parse(text: String, ctx: CatalogContext): JSONObject? {
    val known = ctx.courses.associateBy { normal(it) }
    val teaching = ctx.teaching.mapKeys { it.key.uppercase() }
    val sessions = JSONArray()
    val newCourses = LinkedHashMap<String, Pair<String, String>>()   // code -> (title, programme)
    val rooms = LinkedHashSet<String>()
    val warnings = ArrayList<String>()
    val seen = HashSet<String>()
    val multi = LinkedHashSet<String>()
    val headings = StringBuilder()
    var currentDay: Int? = null
    var skipped = 0
    var parsed = 0

    for ((lineNo, line) in text.lines().withIndex()) {
      val l = line.trim()
      if (l.isEmpty()) continue
      if (l.startsWith("#")) { headings.append(l.trimStart('#', ' ')).append(' '); continue }
      val cells = split(l)
      if (cells.isEmpty() || cells.all { it.all { ch -> ch == '-' || ch == ':' || ch == ' ' || ch == '=' } }) continue
      if (cells.size == 1) {
        dayOf(cells[0])?.let { currentDay = it; return@let }
        if (dayOf(cells[0]) != null) continue
      }
      val low = l.lowercase()
      if (timeFrom(cells.firstOrNull { timeFrom(it) != null }.orEmpty()) == null &&
        (low.contains("venue") || low.contains("course code") || low.contains("room")) && (low.contains("time") || low.contains("day") || low.contains("course"))) continue

      val row = parseCells(cells, currentDay) ?: parseFree(l, currentDay)
      if (row == null) {
        if (lineNo > 3 && cells.size >= 2) skipped++
        else if (cells.size == 1 && l.length in 6..80) headings.append(l).append(' ')
        continue
      }
      val code = known[normal(row.code)] ?: normal(row.code)
      val day = row.day
      if (day == null) { skipped++; continue }
      val key = "$day|${row.start}|${row.end}|$code|${row.venue.lowercase()}"
      if (!seen.add(key)) continue

      if (code !in ctx.courses && code !in newCourses) {
        newCourses[code] = (row.name.ifBlank { code }) to ""
      }
      val title = ctx.titles[code].orEmpty().ifBlank { row.name }
      val lecturers = teaching[code.uppercase()].orEmpty()
      val lecturerName = if (lecturers.size == 1) lecturers[0].name else ""
      if (lecturers.size > 1) multi += code
      val lower = (row.name + " " + title).lowercase()
      val kind = when {
        "lab" in lower || "practical" in lower -> "lab"
        "tutorial" in lower -> "tutorial"
        else -> "lecture"
      }
      if (row.venue.isNotBlank()) rooms += row.venue
      sessions.put(
        JSONObject().put("course", code).put("lecturer", lecturerName).put("day", day).put("start", row.start)
          .put("end", row.end).put("venue", row.venue).put("kind", kind)
      )
      parsed++
    }
    if (parsed == 0) return null

    // Programme of each new course: named in the file's heading, else by its code letters, else by the programme's initials.
    val headingProgramme = fromHeading(headings.toString(), ctx.programmes)
    val coursesJson = JSONArray()
    val undetected = ArrayList<String>()
    for ((code, info) in newCourses) {
      val programme = headingProgramme
        ?: byPrefix(code, ctx.programmeCourses)
        ?: byInitials(code, ctx.programmes)
        ?: ""
      if (programme.isBlank()) undetected += code
      coursesJson.put(JSONObject().put("code", code).put("title", info.first).put("programme", programme))
    }
    if (undetected.isNotEmpty()) {
      warnings += "The programme of ${undetected.joinToString(", ")} could not be found from the file or the codes. They are added without a programme: open the programme and add them."
    }
    if (multi.isNotEmpty()) {
      warnings += "${multi.joinToString(", ")} have more than one lecturer. Choose the lecturer for each of their sessions."
    }
    if (skipped > 0) warnings += "$skipped lines were skipped because they did not match Day, Time, Course Code, Course Name, Venue."

    return JSONObject()
      .put("programmes", JSONArray())
      .put("courses", coursesJson)
      .put("assignments", JSONArray())
      .put("sessions", sessions)
      .put("rooms", JSONArray(rooms.toList()))
      .put("warnings", JSONArray(warnings))
  }
}
