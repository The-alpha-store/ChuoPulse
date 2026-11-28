package com.willykez.campconnect.data.reports

import com.willykez.campconnect.data.model.ClassSession
import com.willykez.campconnect.data.model.SessionStatus

/** One person as the database lets this account see them in a report. */
data class ReportPerson(
  val id: String,
  val name: String,
  val email: String,
  val role: String,
  val number: String,
  val sex: String,
  val programme: String,
  val department: String,
  val cohort: String,
  val courses: List<String>
) {
  val isStudent: Boolean get() = role == "student"
  val isLecturer: Boolean get() = role == "lecturer"
}

/** What to keep. A null filter keeps everyone. */
data class ReportFilter(val programme: String? = null, val course: String? = null, val sex: String? = null) {
  val isEmpty: Boolean get() = programme == null && course == null && sex == null
}

enum class ReportKind { STUDENTS_BY_PROGRAMME, LECTURERS, PROGRAMMES, DEPARTMENTS, ALL_USERS, MY_STUDENTS, TIMETABLE, MY_COURSES, TEACHING_LOAD }

class ReportStyle(val accent: Int, val deep: Int, val campus: String, val t: (String, String) -> String)

object ReportBuilder {
  private val DAYS_EN = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
  private val DAYS_SW = listOf("Jumatatu", "Jumanne", "Jumatano", "Alhamisi", "Ijumaa", "Jumamosi", "Jumapili")

  fun sexLabel(sex: String, t: (String, String) -> String): String = when (sex) {
    "female" -> t("Female", "Mwanamke")
    "male" -> t("Male", "Mwanaume")
    else -> t("Not set", "Haijawekwa")
  }

  private fun baseCode(code: String) = code.substringAfter(':')

  private fun filterText(f: ReportFilter, st: ReportStyle): String {
    if (f.isEmpty) return st.t("Filters: none (everyone)", "Vichujio: hakuna (wote)")
    val parts = ArrayList<String>()
    f.programme?.let { parts += st.t("Programme: $it", "Programu: $it") }
    f.course?.let { parts += st.t("Course: $it", "Somo: $it") }
    f.sex?.let { parts += st.t("Sex: ${sexLabel(it, st.t)}", "Jinsia: ${sexLabel(it, st.t)}") }
    return st.t("Filters: ", "Vichujio: ") + parts.joinToString("  ·  ")
  }

  private fun students(people: List<ReportPerson>, f: ReportFilter) = people.filter { p ->
    p.isStudent &&
      (f.programme == null || p.programme.equals(f.programme, true)) &&
      (f.course == null || p.courses.any { it.equals(f.course, true) }) &&
      (f.sex == null || p.sex == f.sex)
  }

  /** A lecturer matches a programme filter when they teach a course of that programme. */
  private fun lecturers(people: List<ReportPerson>, f: ReportFilter, programmeCourses: Map<String, Set<String>>): List<ReportPerson> {
    val allowed = f.programme?.let { name -> programmeCourses.entries.firstOrNull { it.key.equals(name, true) }?.value.orEmpty().map { it.uppercase() }.toSet() }
    return people.filter { p ->
      p.isLecturer &&
        (allowed == null || p.courses.any { it.uppercase() in allowed }) &&
        (f.course == null || p.courses.any { it.equals(f.course, true) }) &&
        (f.sex == null || p.sex == f.sex)
    }
  }

  private fun counts(list: List<ReportPerson>, st: ReportStyle): List<Pair<String, String>> = listOf(
    st.t("Total", "Jumla") to list.size.toString(),
    st.t("Female", "Wanawake") to list.count { it.sex == "female" }.toString(),
    st.t("Male", "Wanaume") to list.count { it.sex == "male" }.toString(),
    st.t("Not set", "Haijawekwa") to list.count { it.sex != "female" && it.sex != "male" }.toString()
  )

  private fun spec(
    st: ReportStyle, title: String, subtitle: String, summary: List<Pair<String, String>>, filters: String,
    columns: List<PdfColumn>, sections: List<PdfSection>
  ) = PdfSpec(
    title = title, subtitle = subtitle, campus = st.campus, accent = st.accent, deep = st.deep,
    summary = summary, filters = filters, columns = columns, sections = sections,
    emptyText = st.t("Nothing matches these filters.", "Hakuna kinacholingana na vichujio hivi."),
    pageLabel = st.t("Page", "Ukurasa"), ofLabel = st.t("of", "kati ya")
  )

  fun studentsByProgramme(people: List<ReportPerson>, f: ReportFilter, st: ReportStyle, mine: Boolean): PdfSpec {
    val list = students(people, f)
    val groups = list.groupBy { it.programme.ifBlank { st.t("No programme", "Bila programu") } }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    val sections = groups.map { (programme, members) ->
      PdfSection(programme, members.sortedBy { it.name.lowercase() }.mapIndexed { i, p ->
        listOf((i + 1).toString(), p.name, p.number, sexLabel(p.sex, st.t), p.cohort, p.email)
      })
    }
    val summary = listOf(st.t("Programmes", "Programu") to groups.size.toString()) + counts(list, st).take(3)
    return spec(
      st,
      if (mine) st.t("My students", "Wanafunzi wangu") else st.t("Students by programme", "Wanafunzi kwa programu"),
      if (mine) st.t("Students of the programmes and courses I teach", "Wanafunzi wa programu na masomo ninayofundisha")
      else st.t("Every approved student, grouped by programme", "Kila mwanafunzi aliyeidhinishwa, kwa programu"),
      summary, filterText(f, st),
      listOf(
        PdfColumn("#", 0.45f), PdfColumn(st.t("Name", "Jina"), 2.5f), PdfColumn(st.t("Reg. number", "Namba ya usajili"), 1.7f),
        PdfColumn(st.t("Sex", "Jinsia"), 0.9f), PdfColumn(st.t("Cohort", "Kundi"), 1.4f), PdfColumn(st.t("Email", "Barua pepe"), 2.4f)
      ),
      sections
    )
  }

  fun lecturersReport(people: List<ReportPerson>, f: ReportFilter, st: ReportStyle, programmeCourses: Map<String, Set<String>>): PdfSpec {
    val list = lecturers(people, f, programmeCourses)
    val groups = list.groupBy { it.department.ifBlank { st.t("No department", "Bila idara") } }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    val sections = groups.map { (dept, members) ->
      PdfSection(dept, members.sortedBy { it.name.lowercase() }.mapIndexed { i, p ->
        listOf((i + 1).toString(), p.name, p.number, sexLabel(p.sex, st.t), p.courses.joinToString(", "), p.email)
      })
    }
    val summary = listOf(st.t("Departments", "Idara") to groups.size.toString()) + counts(list, st).take(3)
    return spec(
      st, st.t("Lecturers", "Wahadhiri"), st.t("Every approved lecturer, grouped by department", "Kila mhadhiri aliyeidhinishwa, kwa idara"),
      summary, filterText(f, st),
      listOf(
        PdfColumn("#", 0.45f), PdfColumn(st.t("Name", "Jina"), 2.3f), PdfColumn(st.t("Staff number", "Namba ya mfanyakazi"), 1.5f),
        PdfColumn(st.t("Sex", "Jinsia"), 0.9f), PdfColumn(st.t("Courses taught", "Masomo"), 2.2f), PdfColumn(st.t("Email", "Barua pepe"), 2.3f)
      ),
      sections
    )
  }

  fun programmesOverview(people: List<ReportPerson>, f: ReportFilter, st: ReportStyle, programmeCourses: Map<String, Set<String>>): PdfSpec {
    val studs = students(people, f.copy(programme = null))
    val names = (programmeCourses.keys + studs.map { it.programme }.filter { it.isNotBlank() }).distinct()
      .filter { f.programme == null || it.equals(f.programme, true) }.sortedBy { it.lowercase() }
    val rows = names.mapIndexed { i, name ->
      val members = studs.filter { it.programme.equals(name, true) }
      val courses = programmeCourses.entries.firstOrNull { it.key.equals(name, true) }?.value.orEmpty().sorted()
      listOf(
        (i + 1).toString(), name, members.size.toString(), members.count { it.sex == "female" }.toString(),
        members.count { it.sex == "male" }.toString(), courses.size.toString(), courses.joinToString(", ")
      )
    }
    val summary = listOf(
      st.t("Programmes", "Programu") to names.size.toString(),
      st.t("Students", "Wanafunzi") to studs.size.toString(),
      st.t("Female", "Wanawake") to studs.count { it.sex == "female" }.toString(),
      st.t("Male", "Wanaume") to studs.count { it.sex == "male" }.toString()
    )
    return spec(
      st, st.t("All programmes", "Programu zote"), st.t("Students and courses of every programme", "Wanafunzi na masomo ya kila programu"),
      summary, filterText(f.copy(course = null), st),
      listOf(
        PdfColumn("#", 0.45f), PdfColumn(st.t("Programme", "Programu"), 2.6f), PdfColumn(st.t("Students", "Wanafunzi"), 1.0f),
        PdfColumn(st.t("Female", "Wanawake"), 1.0f), PdfColumn(st.t("Male", "Wanaume"), 1.0f),
        PdfColumn(st.t("Courses", "Masomo"), 0.9f), PdfColumn(st.t("Course codes", "Misimbo ya masomo"), 2.6f)
      ),
      listOf(PdfSection(null, rows))
    )
  }

  fun departmentsOverview(people: List<ReportPerson>, f: ReportFilter, st: ReportStyle): PdfSpec {
    val lects = people.filter { it.isLecturer && (f.sex == null || it.sex == f.sex) }
    val groups = lects.groupBy { it.department.ifBlank { st.t("No department", "Bila idara") } }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    val rows = groups.entries.mapIndexed { i, (dept, members) ->
      listOf(
        (i + 1).toString(), dept, members.size.toString(), members.count { it.sex == "female" }.toString(),
        members.count { it.sex == "male" }.toString(), members.flatMap { it.courses }.distinct().sorted().joinToString(", ")
      )
    }
    val summary = listOf(
      st.t("Departments", "Idara") to groups.size.toString(),
      st.t("Lecturers", "Wahadhiri") to lects.size.toString(),
      st.t("Female", "Wanawake") to lects.count { it.sex == "female" }.toString(),
      st.t("Male", "Wanaume") to lects.count { it.sex == "male" }.toString()
    )
    return spec(
      st, st.t("All departments", "Idara zote"), st.t("Lecturers and the courses of every department", "Wahadhiri na masomo ya kila idara"),
      summary, filterText(ReportFilter(sex = f.sex), st),
      listOf(
        PdfColumn("#", 0.45f), PdfColumn(st.t("Department", "Idara"), 2.6f), PdfColumn(st.t("Lecturers", "Wahadhiri"), 1.1f),
        PdfColumn(st.t("Female", "Wanawake"), 1.0f), PdfColumn(st.t("Male", "Wanaume"), 1.0f), PdfColumn(st.t("Courses", "Masomo"), 3.0f)
      ),
      listOf(PdfSection(null, rows))
    )
  }

  fun allUsers(people: List<ReportPerson>, f: ReportFilter, st: ReportStyle, programmeCourses: Map<String, Set<String>>): PdfSpec {
    val studs = students(people, f)
    val lects = lecturers(people, f, programmeCourses)
    fun rows(list: List<ReportPerson>) = list.sortedBy { it.name.lowercase() }.mapIndexed { i, p ->
      listOf((i + 1).toString(), p.name, p.number, sexLabel(p.sex, st.t), p.programme.ifBlank { p.department }, p.email)
    }
    val all = studs + lects
    return spec(
      st, st.t("All users", "Watumiaji wote"), st.t("Every student and lecturer of the campus", "Kila mwanafunzi na mhadhiri wa chuo"),
      listOf(st.t("Students", "Wanafunzi") to studs.size.toString(), st.t("Lecturers", "Wahadhiri") to lects.size.toString(),
        st.t("Female", "Wanawake") to all.count { it.sex == "female" }.toString(), st.t("Male", "Wanaume") to all.count { it.sex == "male" }.toString()),
      filterText(f, st),
      listOf(
        PdfColumn("#", 0.45f), PdfColumn(st.t("Name", "Jina"), 2.4f), PdfColumn(st.t("Number", "Namba"), 1.6f),
        PdfColumn(st.t("Sex", "Jinsia"), 0.9f), PdfColumn(st.t("Programme / department", "Programu / idara"), 2.0f), PdfColumn(st.t("Email", "Barua pepe"), 2.4f)
      ),
      listOf(PdfSection(st.t("Students", "Wanafunzi"), rows(studs)), PdfSection(st.t("Lecturers", "Wahadhiri"), rows(lects)))
    )
  }

  fun timetable(sessions: List<ClassSession>, title: String, who: String, st: ReportStyle, swahili: Boolean): PdfSpec {
    val days = if (swahili) DAYS_SW else DAYS_EN
    val live = sessions.filter { it.status != SessionStatus.CANCELLED }
    val sections = (1..7).mapNotNull { d ->
      val rows = sessions.filter { it.dayOfWeek == d }.sortedBy { it.startTime }.map { s ->
        listOf(
          s.startTime.take(5) + " - " + s.endTime.take(5), baseCode(s.courseCode), s.courseTitle, s.venue, s.lecturerName,
          if (s.status == SessionStatus.CANCELLED) st.t("Cancelled", "Imefutwa") else ""
        )
      }
      if (rows.isEmpty()) null else PdfSection(days[d - 1], rows)
    }
    return spec(
      st, title, who,
      listOf(
        st.t("Sessions", "Vipindi") to live.size.toString(),
        st.t("Courses", "Masomo") to live.map { baseCode(it.courseCode) }.distinct().size.toString(),
        st.t("Days", "Siku") to live.map { it.dayOfWeek }.distinct().size.toString()
      ),
      "",
      listOf(
        PdfColumn(st.t("Time", "Muda"), 1.5f), PdfColumn(st.t("Code", "Msimbo"), 1.0f), PdfColumn(st.t("Course", "Somo"), 2.6f),
        PdfColumn(st.t("Venue", "Ukumbi"), 1.5f), PdfColumn(st.t("Lecturer", "Mhadhiri"), 1.8f), PdfColumn(st.t("Note", "Maelezo"), 1.0f)
      ),
      sections
    )
  }

  /** The courses a student is enrolled in, or a lecturer teaches, with their lecturers and weekly sessions. */
  fun courses(codes: Set<String>, titles: Map<String, String>, sessions: List<ClassSession>, st: ReportStyle, title: String, subtitle: String, studentCounts: Map<String, Int>?): PdfSpec {
    val rows = codes.sorted().mapIndexed { i, code ->
      val mine = sessions.filter { baseCode(it.courseCode).equals(code, true) && it.status != SessionStatus.CANCELLED }
      val base = listOf(
        (i + 1).toString(), code, titles[code].orEmpty(), mine.map { it.lecturerName }.distinct().joinToString(", "), mine.size.toString()
      )
      if (studentCounts != null) base + (studentCounts[code] ?: 0).toString() else base
    }
    val columns = listOf(
      PdfColumn("#", 0.45f), PdfColumn(st.t("Code", "Msimbo"), 1.1f), PdfColumn(st.t("Course", "Somo"), 3.0f),
      PdfColumn(st.t("Lecturers", "Wahadhiri"), 2.2f), PdfColumn(st.t("Sessions", "Vipindi"), 1.0f)
    ) + (if (studentCounts != null) listOf(PdfColumn(st.t("Students", "Wanafunzi"), 1.0f)) else emptyList())
    return spec(
      st, title, subtitle,
      listOf(st.t("Courses", "Masomo") to codes.size.toString(), st.t("Sessions a week", "Vipindi kwa wiki") to rows.sumOf { it[4].toIntOrNull() ?: 0 }.toString()),
      "", columns, listOf(PdfSection(null, rows))
    )
  }
}
