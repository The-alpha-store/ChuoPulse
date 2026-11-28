package com.willykez.campconnect.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.willykez.campconnect.data.model.Role
import com.willykez.campconnect.data.reports.PdfReport
import com.willykez.campconnect.data.reports.ReportBuilder
import com.willykez.campconnect.data.reports.ReportFiles
import com.willykez.campconnect.data.reports.ReportFilter
import com.willykez.campconnect.data.reports.ReportKind
import com.willykez.campconnect.data.reports.ReportPerson
import com.willykez.campconnect.data.reports.ReportStyle
import com.willykez.campconnect.ui.theme.Brand
import com.willykez.campconnect.ui.theme.BrandDeep
import com.willykez.campconnect.ui.theme.BrandLight
import com.willykez.campconnect.ui.theme.BrandPrimary
import com.willykez.campconnect.ui.theme.DangerRed
import com.willykez.campconnect.viewmodel.CampusViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class ReportItem(
  val kind: ReportKind,
  val titleEn: String, val titleSw: String,
  val descEn: String, val descSw: String,
  val icon: ImageVector,
  val usesFilters: Boolean
)

/** PDF reports. Each role sees only the reports it is allowed to make; the people in them are chosen by the database. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: CampusViewModel, modifier: Modifier = Modifier) {
  val user by viewModel.currentUser.collectAsState()
  val sessions by viewModel.sessions.collectAsState()
  val universities by viewModel.universities.collectAsState()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val sw = viewModel.t("en", "sw") == "sw"
  fun tr(en: String, s: String) = if (sw) s else en

  val isAdmin = user.role == Role.ADMIN
  val isLecturer = user.role == Role.LECTURER
  val needsPeople = isAdmin || isLecturer

  var people by remember { mutableStateOf<List<ReportPerson>>(emptyList()) }
  var loading by remember { mutableStateOf(needsPeople) }
  var failed by remember { mutableStateOf(false) }
  var programme by remember { mutableStateOf<String?>(null) }
  var course by remember { mutableStateOf<String?>(null) }
  var sex by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf<ReportKind?>(null) }
  var ready by remember { mutableStateOf<Pair<String, File>?>(null) }
  var mySex by remember(user.id) { mutableStateOf(user.sex) }

  LaunchedEffect(user.id, user.role) {
    if (needsPeople) {
      loading = true
      val r = viewModel.loadReportPeople()
      if (r == null) failed = true else { people = r; failed = false }
      loading = false
    }
  }

  // Derived locally from the loaded people so we don't depend on view-model helpers.
  // ReportPerson exposes `programme: String` and `courses: List<String>`.
  val programmeCourses: Map<String, List<String>> = remember(people) {
    people
      .filter { it.isStudent }
      .groupBy { it.programme }
      .mapValues { (_, list) -> list.flatMap { it.courses }.distinct() }
  }
  val titles: Map<String, String> = remember(people) {
    people.flatMap { it.courses }.distinct().associateWith { it }
  }
  val campusName = universities.firstOrNull { it.id == user.universityId }?.name ?: "ChuoPulse"

  val items: List<ReportItem> = when {
    isAdmin -> listOf(
      ReportItem(ReportKind.STUDENTS_BY_PROGRAMME, "Students by programme", "Wanafunzi kwa programu", "All students grouped by programme. Filter by programme, course and sex.", "Wanafunzi wote kwa programu. Chuja kwa programu, somo na jinsia.", Icons.Default.School, true),
      ReportItem(ReportKind.LECTURERS, "Lecturers", "Wahadhiri", "All lecturers grouped by department, with their courses.", "Wahadhiri wote kwa idara, pamoja na masomo yao.", Icons.Default.Groups, true),
      ReportItem(ReportKind.PROGRAMMES, "All programmes", "Programu zote", "Students, women, men and courses of every programme.", "Wanafunzi, wanawake, wanaume na masomo ya kila programu.", Icons.Default.School, true),
      ReportItem(ReportKind.DEPARTMENTS, "All departments", "Idara zote", "Lecturers and courses of every department.", "Wahadhiri na masomo ya kila idara.", Icons.Default.Groups, true),
      ReportItem(ReportKind.ALL_USERS, "All users", "Watumiaji wote", "Every student and lecturer of the campus.", "Kila mwanafunzi na mhadhiri wa chuo.", Icons.Default.Groups, true),
      ReportItem(ReportKind.TIMETABLE, "Campus timetable", "Ratiba ya chuo", "The full timetable, day by day.", "Ratiba kamili, siku kwa siku.", Icons.Default.CalendarToday, false)
    )
    isLecturer -> listOf(
      ReportItem(ReportKind.MY_STUDENTS, "My students", "Wanafunzi wangu", "Students of the programmes and courses I teach. Filter by programme, course and sex.", "Wanafunzi wa programu na masomo ninayofundisha. Chuja kwa programu, somo na jinsia.", Icons.Default.School, true),
      ReportItem(ReportKind.TEACHING_LOAD, "My courses", "Masomo yangu", "My courses, number of students and weekly sessions.", "Masomo yangu, idadi ya wanafunzi na vipindi vya wiki.", Icons.Default.Groups, false),
      ReportItem(ReportKind.TIMETABLE, "My timetable", "Ratiba yangu", "My classes, day by day.", "Vipindi vyangu, siku kwa siku.", Icons.Default.CalendarToday, false)
    )
    else -> listOf(
      ReportItem(ReportKind.TIMETABLE, "My timetable", "Ratiba yangu", "My classes, day by day.", "Vipindi vyangu, siku kwa siku.", Icons.Default.CalendarToday, false),
      ReportItem(ReportKind.MY_COURSES, "My courses", "Masomo yangu", "The courses I study, with their lecturers.", "Masomo ninayosoma, pamoja na wahadhiri wao.", Icons.Default.School, false)
    )
  }

  val style = ReportStyle(
    accent = Brand.palette.accentLight.toArgb(), deep = Brand.palette.deep.toArgb(), campus = campusName,
    t = { en, s -> if (sw) s else en }
  )
  val filter = ReportFilter(programme, course, sex)

  fun make(item: ReportItem) {
    scope.launch {
      busy = item.kind
      try {
        val result = withContext(Dispatchers.Default) {
          val spec = when (item.kind) {
            ReportKind.STUDENTS_BY_PROGRAMME -> ReportBuilder.studentsByProgramme(people, filter, style, mine = false)
            ReportKind.MY_STUDENTS -> ReportBuilder.studentsByProgramme(people, filter, style, mine = true)
            ReportKind.LECTURERS -> ReportBuilder.lecturersReport(people, filter, style, programmeCourses)
            ReportKind.PROGRAMMES -> ReportBuilder.programmesOverview(people, filter, style, programmeCourses)
            ReportKind.DEPARTMENTS -> ReportBuilder.departmentsOverview(people, filter, style)
            ReportKind.ALL_USERS -> ReportBuilder.allUsers(people, filter, style, programmeCourses)
            ReportKind.TIMETABLE -> ReportBuilder.timetable(
              sessions, if (isAdmin) tr("Campus timetable", "Ratiba ya chuo") else tr("My timetable", "Ratiba yangu"),
              if (isAdmin) campusName else user.fullName, style, sw
            )
            ReportKind.MY_COURSES -> ReportBuilder.courses(
              user.enrolledCourses, titles, sessions, style, tr("My courses", "Masomo yangu"), user.fullName, null
            )
            ReportKind.TEACHING_LOAD -> ReportBuilder.courses(
              user.teachesCourses, titles, sessions, style, tr("My courses", "Masomo yangu"), user.fullName,
              user.teachesCourses.associateWith { code -> people.count { p -> p.courses.any { it.equals(code, true) } } }
            )
          }
          val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
          val file = ReportFiles.file(context, "${spec.title}-$stamp")
          PdfReport.render(spec, file)
          spec.title to file
        }
        ready = result
      } catch (e: Exception) {
        viewModel.showMessage(tr("The report could not be made. Try again.", "Ripoti haikuweza kutengenezwa. Jaribu tena."))
      }
      busy = null
    }
  }

  val programmeOptions = remember(people, programmeCourses) {
    (people.filter { it.isStudent }.map { it.programme } + programmeCourses.keys).filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() }
  }
  val courseOptions = remember(people) { people.flatMap { it.courses }.distinct().sortedBy { it.lowercase() } }
  val withoutSex = people.count { it.sex != "female" && it.sex != "male" }

  Column(
    modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Box(
      Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(BrandDeep, BrandPrimary, BrandLight)), RoundedCornerShape(20.dp)).padding(20.dp)
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.PictureAsPdf, null, tint = Color.White, modifier = Modifier.size(26.dp))
          Spacer(Modifier.width(10.dp))
          Text(tr("Reports", "Ripoti"), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(
          when {
            isAdmin -> tr("Make a PDF of your campus: students, lecturers, programmes and departments.", "Tengeneza PDF ya chuo chako: wanafunzi, wahadhiri, programu na idara.")
            isLecturer -> tr("Make a PDF of the students you teach, your courses and your timetable.", "Tengeneza PDF ya wanafunzi unaowafundisha, masomo yako na ratiba yako.")
            else -> tr("Download your timetable and your courses as a PDF.", "Pakua ratiba na masomo yako kama PDF.")
          },
          color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium
        )
      }
    }

    if (loading) {
      Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(color = BrandPrimary) }
    }
    if (failed) {
      Text(
        tr("The people could not be loaded. Check your connection and open Reports again. Timetable reports still work.", "Watu hawakupatikana. Angalia mtandao kisha fungua Ripoti tena."),
        color = DangerRed, style = MaterialTheme.typography.bodySmall
      )
    }

    if (needsPeople && !loading && !failed) {
      Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(tr("Filters", "Vichujio"), fontWeight = FontWeight.Bold)
          ChipRow(tr("Programme", "Programu"), programmeOptions.map { it to it }, programme, tr("All", "Zote")) { programme = it }
          if (courseOptions.isNotEmpty()) {
            ChipRow(tr("Course", "Somo"), courseOptions.map { it to it }, course, tr("All", "Yote")) { course = it }
          }
          ChipRow(
            tr("Sex", "Jinsia"), listOf("female" to tr("Female", "Mwanamke"), "male" to tr("Male", "Mwanaume")), sex, tr("All", "Wote")
          ) { sex = it }
          Text(
            tr("${people.size} people can go in a report.", "Watu ${people.size} wanaweza kuingia kwenye ripoti.") +
              if (withoutSex > 0) " " + tr("$withoutSex have no sex recorded and show as \"Not set\".", "$withoutSex hawana jinsia na wanaonekana \"Haijawekwa\".") else "",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    if (isLecturer && !loading && !failed) {
      val shown = people.filter { p ->
        p.isStudent &&
          (programme == null || p.programme.equals(programme, true)) &&
          (course == null || p.courses.any { it.equals(course, true) }) &&
          (sex == null || p.sex == sex)
      }.sortedBy { it.name.lowercase() }
      Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(tr("My students (${shown.size})", "Wanafunzi wangu (${shown.size})"), fontWeight = FontWeight.Bold)
          if (shown.isEmpty()) {
            Text(
              tr("No students yet. They appear when you are assigned courses and students join those programmes.", "Bado hakuna wanafunzi. Wataonekana ukipewa masomo na wanafunzi wakijiunga na programu hizo."),
              style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          shown.take(150).forEach { p ->
            Column(Modifier.fillMaxWidth()) {
              Text(p.name, fontWeight = FontWeight.SemiBold)
              Text(
                listOf(p.number, p.programme, ReportBuilder.sexLabel(p.sex) { en, s -> if (sw) s else en }).filter { it.isNotBlank() }.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          if (shown.size > 150) {
            Text(tr("…and ${shown.size - 150} more. Make the PDF to see everyone.", "…na wengine ${shown.size - 150}. Tengeneza PDF kuwaona wote."), style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }

    items.forEach { item ->
      val disabled = item.usesFilters && (loading || failed)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("report_${item.kind.name.lowercase()}")
      ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(42.dp).background(BrandPrimary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(item.icon, null, tint = BrandPrimary)
          }
          Spacer(Modifier.width(12.dp))
          Column(Modifier.weight(1f)) {
            Text(tr(item.titleEn, item.titleSw), fontWeight = FontWeight.Bold)
            Text(tr(item.descEn, item.descSw), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Spacer(Modifier.width(8.dp))
          if (busy == item.kind) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = BrandPrimary)
          } else {
            Button(
              onClick = { make(item) }, enabled = busy == null && !disabled, shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) { Text("PDF") }
          }
        }
      }
    }

    if (!isAdmin) {
      Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(tr("My details", "Taarifa zangu"), fontWeight = FontWeight.Bold)
          Text(
            tr("Your campus uses sex in its reports. Choose yours so it is counted correctly.", "Chuo chako kinatumia jinsia kwenye ripoti. Chagua yako ili ihesabiwe sawa."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("female" to tr("Female", "Mwanamke"), "male" to tr("Male", "Mwanaume")).forEach { (id, label) ->
              FilterChip(
                selected = mySex == id,
                onClick = { scope.launch { if (viewModel.setMySex(id) == null) mySex = id } },
                label = { Text(label) }, modifier = Modifier.testTag("my_sex_$id")
              )
            }
          }
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }

  ready?.let { (title, file) ->
    AlertDialog(
      onDismissRequest = { ready = null },
      title = { Text(tr("Your PDF is ready", "PDF yako iko tayari")) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(title, fontWeight = FontWeight.SemiBold)
          Button(
            onClick = {
              if (!ReportFiles.open(context, file)) viewModel.showMessage(tr("No PDF viewer found. Use Download or Share.", "Hakuna programu ya kusoma PDF. Tumia Pakua au Shiriki."))
            },
            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary), modifier = Modifier.fillMaxWidth()
          ) { Text(tr("Open", "Fungua")) }
          OutlinedButton(
            onClick = {
              if (ReportFiles.saveToDownloads(context, file)) viewModel.showMessage(tr("Saved in Downloads/ChuoPulse.", "Imehifadhiwa kwenye Downloads/ChuoPulse."))
              else ReportFiles.share(context, file)
            },
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
          ) { Text(tr("Download", "Pakua")) }
          OutlinedButton(onClick = { ReportFiles.share(context, file) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(tr("Share", "Shiriki"))
          }
        }
      },
      confirmButton = { TextButton(onClick = { ready = null }) { Text(tr("Close", "Funga")) } }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(label: String, options: List<Pair<String, String>>, selected: String?, allLabel: String, onSelect: (String?) -> Unit) {
  Column {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      item { FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(allLabel) }) }
      items(options.size) { i ->
        val (id, text) = options[i]
        FilterChip(selected = selected == id, onClick = { onSelect(if (selected == id) null else id) }, label = { Text(text) })
      }
    }
  }
}