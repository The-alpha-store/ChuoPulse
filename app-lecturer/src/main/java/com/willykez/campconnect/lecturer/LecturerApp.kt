@file:OptIn(ExperimentalMaterial3Api::class)
package com.willykez.campconnect.lecturer

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willykez.campconnect.data.docs.DocumentAnalysisService
import com.willykez.campconnect.data.local.ProcessedDocumentRepository
import com.willykez.campconnect.data.model.AccountStatus
import com.willykez.campconnect.data.model.Announcement
import com.willykez.campconnect.data.model.AppLanguage
import com.willykez.campconnect.data.model.AttachmentFile
import com.willykez.campconnect.data.model.DeadlineItem
import com.willykez.campconnect.data.model.Role
import com.willykez.campconnect.data.model.StudentSubmission
import com.willykez.campconnect.ui.components.AddDeadlineDialog
import com.willykez.campconnect.ui.components.AnalysisState
import com.willykez.campconnect.ui.components.AnnouncementDetailDialog
import com.willykez.campconnect.ui.components.CancelSessionDialog
import com.willykez.campconnect.ui.components.ChangeVenueDialog
import com.willykez.campconnect.ui.components.DocumentViewerDialog
import com.willykez.campconnect.ui.components.EditAnnouncementDialog
import com.willykez.campconnect.ui.components.EditDeadlineSheet
import com.willykez.campconnect.ui.components.GradeSubmissionDialog
import com.willykez.campconnect.ui.components.PublishAnnouncementDialog
import com.willykez.campconnect.ui.components.UploadMaterialDialog
import com.willykez.campconnect.ui.components.ViewerAnalysis
import com.willykez.campconnect.ui.screens.AuthGate
import com.willykez.campconnect.ui.screens.DeadlinesScreen
import com.willykez.campconnect.ui.screens.HomeScreen
import com.willykez.campconnect.ui.screens.MaintenanceScreen
import com.willykez.campconnect.ui.screens.MaterialsScreen
import com.willykez.campconnect.ui.screens.NotificationsScreen
import com.willykez.campconnect.ui.screens.ProcessedDocumentsScreen
import com.willykez.campconnect.ui.screens.ProfileScreen
import com.willykez.campconnect.ui.screens.QuizzesScreen
import com.willykez.campconnect.ui.screens.SplashScreen
import com.willykez.campconnect.ui.screens.SubmissionsScreen
import com.willykez.campconnect.ui.screens.TimetableScreen
import com.willykez.campconnect.ui.screens.WaitingRoomScreen
import com.willykez.campconnect.ui.screens.WrongAppScreen
import com.willykez.campconnect.ui.theme.AppThemeSettings
import com.willykez.campconnect.ui.theme.CampusConnectTheme
import com.willykez.campconnect.ui.theme.DangerRed
import com.willykez.campconnect.ui.theme.DeepBlue
import com.willykez.campconnect.ui.theme.WarmAmber
import com.willykez.campconnect.viewmodel.CampusViewModel

private enum class LecturerTab {
  HOME,
  TIMETABLE,
  MATERIALS,
  ASSESSMENTS,
  SUBMISSIONS,
  PROFILE,
  PROCESSED_DOCS,
  NOTIFICATIONS
}

/**
 * Root composable for the Lecturer app (`com.willykez.campconnect.lecturer`).
 * Tailored specifically for educators:
 * - Teaching timetable & class session management (change venue, cancel)
 * - Course notes & study material upload and management
 * - Assignment deadlines creation & quiz management
 * - Student submissions grading & feedback
 * - Course announcement publishing
 */
@Composable
fun LecturerApp(viewModel: CampusViewModel = viewModel()) {
  val session by viewModel.session.collectAsState()
  val user by viewModel.currentUser.collectAsState()
  val restoring by viewModel.restoring.collectAsState()
  val swahili by viewModel.appLanguage.collectAsState()
  val paused by viewModel.campusPaused.collectAsState()
  var splashDone by rememberSaveable { mutableStateOf(false) }

  val rootContext = LocalContext.current
  var lastBackAt by remember { mutableStateOf(0L) }
  BackHandler(enabled = splashDone && !restoring) {
    val now = System.currentTimeMillis()
    if (now - lastBackAt < 2000L) {
      (rootContext as? android.app.Activity)?.finish()
    } else {
      lastBackAt = now
      Toast.makeText(
        rootContext,
        viewModel.t("Press back again to exit", "Bonyeza tena kurudi nyuma ili kutoka"),
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  CampusConnectTheme {
    when {
      !splashDone || restoring -> SplashScreen(
        swahili = swahili == AppLanguage.SWAHILI,
        onFinished = { splashDone = true }
      )
      session == null -> AuthGate(viewModel)
      user.status != AccountStatus.APPROVED -> WaitingRoomScreen(viewModel)
      // Dedicated Role guard: Lecturers only
      user.role != Role.LECTURER -> WrongAppScreen(
        viewModel = viewModel,
        expectedRoleLabel = viewModel.t("Lecturer", "Mhadhiri")
      )
      paused -> MaintenanceScreen(viewModel)
      else -> LecturerMainContent(viewModel)
    }
  }
}

@Composable
private fun LecturerMainContent(viewModel: CampusViewModel) {
  val context = LocalContext.current
  val systemDark = isSystemInDarkTheme()
  val message by viewModel.message.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(message) {
    message?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.messageShown()
    }
  }

  var currentTab by rememberSaveable { mutableStateOf(LecturerTab.HOME) }
  BackHandler(enabled = currentTab != LecturerTab.HOME) { currentTab = LecturerTab.HOME }

  val currentUser by viewModel.currentUser.collectAsState()
  val isOffline by viewModel.isOffline.collectAsState()
  val syncing by viewModel.syncing.collectAsState()
  val unreadNotifs by viewModel.unreadNotificationsCount.collectAsState()
  val submissions by viewModel.submissions.collectAsState()
  val announcements by viewModel.announcements.collectAsState()
  val sessions by viewModel.sessions.collectAsState()
  val remindedIds by viewModel.remindedAnnouncements.collectAsState()
  val pendingReview = submissions.count { it.grade == null }

  var selectedAnnouncementId by remember { mutableStateOf<String?>(null) }
  var showPublishDialog by remember { mutableStateOf(false) }
  var showUploadDialog by remember { mutableStateOf(false) }
  var showAddDeadlineDialog by remember { mutableStateOf(false) }
  var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
  var editingDeadline by remember { mutableStateOf<DeadlineItem?>(null) }
  var gradingSubmission by remember { mutableStateOf<StudentSubmission?>(null) }
  var venueChangeSessionId by remember { mutableStateOf<String?>(null) }
  var cancelSessionId by remember { mutableStateOf<String?>(null) }

  var viewingDocument by remember { mutableStateOf<AttachmentFile?>(null) }
  var viewingDocAuthor by remember { mutableStateOf<String?>(null) }
  var viewingDocCourse by remember { mutableStateOf<String?>(null) }

  var assessmentSubTab by rememberSaveable { mutableIntStateOf(0) }

  Scaffold(
    modifier = Modifier.fillMaxSize().testTag("lecturer_main_scaffold"),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DeepBlue),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "ChuoPulse Lecturer",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(WarmAmber.copy(alpha = 0.18f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "LECTURER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309),
                    fontSize = 9.sp
                  )
                }
              }
              Text(
                text = if (isOffline) viewModel.getString("offline_notice")
                else if (syncing) viewModel.t("Syncing…", "Inasawazisha…")
                else "${currentUser.fullName.split(" ").firstOrNull() ?: ""} • Teaching Portal",
                style = MaterialTheme.typography.labelSmall,
                color = if (isOffline) WarmAmber else if (syncing) DeepBlue else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          var menuOpen by remember { mutableStateOf(false) }
          val isDark = AppThemeSettings.mode == com.willykez.campconnect.ui.theme.AppThemeMode.DARK ||
            (AppThemeSettings.mode == com.willykez.campconnect.ui.theme.AppThemeMode.SYSTEM && systemDark)

          // Notifications
          IconButton(onClick = { currentTab = LecturerTab.NOTIFICATIONS }) {
            BadgedBox(
              badge = {
                if (unreadNotifs > 0) {
                  Badge(containerColor = DeepBlue, contentColor = Color.White) { Text("$unreadNotifs") }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = if (currentTab == LecturerTab.NOTIFICATIONS) DeepBlue else MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // Profile & Settings Menu
          Box {
            IconButton(onClick = { menuOpen = true }) {
              BadgedBox(
                badge = {
                  if (pendingReview > 0) {
                    Badge(containerColor = WarmAmber, contentColor = Color.White) { Text("$pendingReview") }
                  }
                }
              ) {
                Box(
                  modifier = Modifier.size(30.dp).clip(CircleShape).background(DeepBlue),
                  contentAlignment = Alignment.Center
                ) {
                  Text(currentUser.avatarInitials.ifBlank { "?" }, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
              Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(currentUser.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(currentUser.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              HorizontalDivider()
              DropdownMenuItem(
                text = { Text(viewModel.t("My profile", "Wasifu wangu")) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                onClick = { menuOpen = false; currentTab = LecturerTab.PROFILE }
              )
              DropdownMenuItem(
                text = { Text(viewModel.t("AI summaries", "Muhtasari wa AI")) },
                leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                onClick = { menuOpen = false; currentTab = LecturerTab.PROCESSED_DOCS }
              )
              DropdownMenuItem(
                text = { Text(if (isDark) viewModel.t("Light mode", "Mwanga") else viewModel.t("Dark mode", "Giza")) },
                leadingIcon = { Icon(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = null) },
                onClick = { AppThemeSettings.toggle(context, systemDark) }
              )
              HorizontalDivider()
              DropdownMenuItem(
                text = { Text(viewModel.t("Sign out", "Toka"), color = DangerRed) },
                leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = DangerRed) },
                onClick = { menuOpen = false; viewModel.signOut() }
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
      ) {
        NavigationBarItem(
          selected = currentTab == LecturerTab.HOME,
          onClick = { currentTab = LecturerTab.HOME },
          icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
          label = { Text(viewModel.getString("tab_home")) },
          colors = NavigationBarItemDefaults.colors(indicatorColor = DeepBlue.copy(alpha = 0.15f))
        )

        NavigationBarItem(
          selected = currentTab == LecturerTab.TIMETABLE,
          onClick = { currentTab = LecturerTab.TIMETABLE },
          icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Classes") },
          label = { Text(viewModel.t("Classes", "Vipindi")) },
          colors = NavigationBarItemDefaults.colors(indicatorColor = DeepBlue.copy(alpha = 0.15f))
        )

        NavigationBarItem(
          selected = currentTab == LecturerTab.MATERIALS,
          onClick = { currentTab = LecturerTab.MATERIALS },
          icon = { Icon(Icons.Default.Folder, contentDescription = "Materials") },
          label = { Text(viewModel.getString("tab_materials")) },
          colors = NavigationBarItemDefaults.colors(indicatorColor = DeepBlue.copy(alpha = 0.15f))
        )

        NavigationBarItem(
          selected = currentTab == LecturerTab.ASSESSMENTS,
          onClick = { currentTab = LecturerTab.ASSESSMENTS },
          icon = { Icon(Icons.Default.Assignment, contentDescription = "Assessments") },
          label = { Text(viewModel.t("Assessments", "Tathmini")) },
          colors = NavigationBarItemDefaults.colors(indicatorColor = DeepBlue.copy(alpha = 0.15f))
        )

        NavigationBarItem(
          selected = currentTab == LecturerTab.SUBMISSIONS,
          onClick = { currentTab = LecturerTab.SUBMISSIONS },
          icon = {
            BadgedBox(
              badge = {
                if (pendingReview > 0) {
                  Badge(containerColor = WarmAmber, contentColor = Color.White) { Text("$pendingReview") }
                }
              }
            ) {
              Icon(Icons.Default.UploadFile, contentDescription = "Grading")
            }
          },
          label = { Text(viewModel.t("Grading", "Kusahihisha")) },
          colors = NavigationBarItemDefaults.colors(indicatorColor = DeepBlue.copy(alpha = 0.15f))
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Crossfade(targetState = currentTab, animationSpec = tween(180), label = "lecturer_tab") { tab ->
        when (tab) {
          LecturerTab.HOME -> HomeScreen(
            viewModel = viewModel,
            onNavigateToNotifications = { currentTab = LecturerTab.NOTIFICATIONS },
            onOpenAnnouncementDetail = { selectedAnnouncementId = it },
            onOpenPublishDialog = { showPublishDialog = true },
            onOpenChangeVenueDialog = { venueChangeSessionId = it },
            onOpenUploadDialog = { showUploadDialog = true },
            onOpenAddDeadlineDialog = { showAddDeadlineDialog = true }
          )

          LecturerTab.TIMETABLE -> TimetableScreen(
            viewModel = viewModel,
            onOpenChangeVenueDialog = { venueChangeSessionId = it },
            onOpenCancelDialog = { cancelSessionId = it }
          )

          LecturerTab.MATERIALS -> MaterialsScreen(
            viewModel = viewModel,
            onOpenUploadDialog = { showUploadDialog = true },
            onOpenProcessedDocuments = { currentTab = LecturerTab.PROCESSED_DOCS },
            onPreviewDocument = { file, author, course ->
              viewingDocument = file
              viewingDocAuthor = author
              viewingDocCourse = course
            }
          )

          LecturerTab.ASSESSMENTS -> {
            Column(modifier = Modifier.fillMaxSize()) {
              TabRow(
                selectedTabIndex = assessmentSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = DeepBlue
              ) {
                Tab(
                  selected = assessmentSubTab == 0,
                  onClick = { assessmentSubTab = 0 },
                  text = { Text(viewModel.t("Assignments & Deadlines", "Kazi & Tarehe za Mwisho"), fontWeight = if (assessmentSubTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                  selected = assessmentSubTab == 1,
                  onClick = { assessmentSubTab = 1 },
                  text = { Text(viewModel.t("Quizzes", "Majaribio"), fontWeight = if (assessmentSubTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
              }
              if (assessmentSubTab == 0) {
                DeadlinesScreen(
                  viewModel = viewModel,
                  onOpenAddDeadlineDialog = { showAddDeadlineDialog = true },
                  onSubmitWorkClick = { /* Lecturers grade submissions, they do not submit work */ },
                  onEditDeadline = { dl -> editingDeadline = dl },
                  onOpenAttachment = { file, author, course ->
                    viewingDocument = file
                    viewingDocAuthor = author
                    viewingDocCourse = course
                  }
                )
              } else {
                QuizzesScreen(viewModel = viewModel)
              }
            }
          }

          LecturerTab.SUBMISSIONS -> SubmissionsScreen(
            viewModel = viewModel,
            onPreviewDocument = { file ->
              viewingDocument = file
              viewingDocAuthor = null
              viewingDocCourse = null
            },
            onGradeSubmission = { sub -> gradingSubmission = sub }
          )

          LecturerTab.PROFILE -> ProfileScreen(viewModel = viewModel)
          LecturerTab.PROCESSED_DOCS -> ProcessedDocumentsScreen(viewModel = viewModel)
          LecturerTab.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
        }
      }

      // Dialogs for Lecturer
      selectedAnnouncementId?.let { id ->
        val announcement = announcements.find { it.id == id }
        if (announcement != null) {
          AnnouncementDetailDialog(
            announcement = announcement,
            currentUserRole = Role.LECTURER,
            canModerate = viewModel.canEditOrDelete(announcement.authorId),
            onDismiss = { selectedAnnouncementId = null },
            onPreviewAttachment = { file ->
              viewingDocument = file
              viewingDocAuthor = announcement.authorName
              viewingDocCourse = announcement.audienceName
            },
            onEditClick = {
              selectedAnnouncementId = null
              editingAnnouncement = announcement
            },
            onDeleteClick = {
              viewModel.deleteAnnouncement(announcement.id)
              selectedAnnouncementId = null
            },
            onToggleApproval = { status -> viewModel.setAnnouncementApproval(announcement.id, status) },
            showReadStats = viewModel.canEditOrDelete(announcement.authorId),
            reminderSent = announcement.id in remindedIds,
            onRemindClick = { viewModel.remindUnread(announcement.id) },
            onRetractClick = {
              viewModel.retractAnnouncement(announcement.id)
              selectedAnnouncementId = null
            }
          )
        }
      }

      if (showPublishDialog) {
        PublishAnnouncementDialog(
          options = viewModel.audienceOptions(),
          urgentLeft = viewModel.urgentLeftToday(),
          canUseUrgentFor = { viewModel.canUseUrgentFor(it) },
          onDismiss = { showPublishDialog = false },
          lecturersFor = { viewModel.lecturersFor(it) },
          onPublish = { title, body, audienceType, audienceName, audienceCode, priority, attachment, responsible ->
            viewModel.publishAnnouncement(title, body, audienceType, audienceName, audienceCode, priority, attachment, responsible)
          },
          onPreviewAttachment = { file -> viewingDocument = file }
        )
      }

      editingAnnouncement?.let { ann ->
        EditAnnouncementDialog(
          announcement = ann,
          onDismiss = { editingAnnouncement = null },
          onSave = { newTitle, newBody, newPriority ->
            viewModel.updateAnnouncement(ann.id, newTitle, newBody, newPriority)
          }
        )
      }

      if (showUploadDialog) {
        UploadMaterialDialog(
          onDismiss = { showUploadDialog = false },
          defaultCourseCode = currentUser.teachesCourses.minOrNull() ?: "CS210",
          onUpload = { title, code, courseTitle, desc, attachment ->
            viewModel.uploadMaterial(title, code, courseTitle, desc, attachment)
          },
          onPreviewAttachment = { file -> viewingDocument = file }
        )
      }

      if (showAddDeadlineDialog) {
        AddDeadlineDialog(
          onDismiss = { showAddDeadlineDialog = false },
          courses = viewModel.schedulableCourses(),
          defaultCourseCode = currentUser.teachesCourses.minOrNull() ?: "",
          onAdd = { code, courseTitle, title, instructions, dueDateTime, formats, attachment ->
            viewModel.addDeadline(code, courseTitle, title, instructions, dueDateTime, formats, attachment)
          },
          onPreviewAttachment = { file -> viewingDocument = file }
        )
      }

      editingDeadline?.let { dl ->
        EditDeadlineSheet(
          deadline = dl,
          onDismiss = { editingDeadline = null },
          onSave = { title, instructions, due, formats, attachment, clear ->
            viewModel.updateDeadline(dl.id, title, instructions, due, formats, attachment, clear)
          },
          onPreviewAttachment = { file -> viewingDocument = file }
        )
      }

      gradingSubmission?.let { sub ->
        GradeSubmissionDialog(
          submission = sub,
          onDismiss = { gradingSubmission = null },
          onPreviewDocument = { file ->
            viewingDocument = file
            viewingDocAuthor = sub.studentName
            viewingDocCourse = sub.courseCode
          },
          onGradeSubmitted = { grade, feedback ->
            viewModel.gradeStudentWork(sub.id, grade, feedback)
          }
        )
      }

      venueChangeSessionId?.let { sId ->
        val session = sessions.find { it.id == sId }
        if (session != null) {
          ChangeVenueDialog(
            session = session,
            onDismiss = { venueChangeSessionId = null },
            onConfirm = { newVenue, reason ->
              viewModel.updateSessionVenue(sId, newVenue, reason)
            }
          )
        }
      }

      cancelSessionId?.let { sId ->
        val session = sessions.find { it.id == sId }
        if (session != null) {
          CancelSessionDialog(
            session = session,
            onDismiss = { cancelSessionId = null },
            onConfirm = { reason ->
              viewModel.cancelSession(sId, reason)
            }
          )
        }
      }

      viewingDocument?.let { doc ->
        val appContext = LocalContext.current
        val analysisService = remember(appContext) { DocumentAnalysisService.get(appContext) }
        val libraryFlow = remember(appContext) { ProcessedDocumentRepository(appContext).observeDocuments() }
        val library by libraryFlow.collectAsState(initial = emptyList())
        var analysisId by remember(doc) { mutableStateOf<String?>(null) }
        val entry = library.firstOrNull { it.id == analysisId }
        DocumentViewerDialog(
          attachment = doc,
          authorOrUploaderName = viewingDocAuthor,
          courseCode = viewingDocCourse,
          onDismiss = {
            viewingDocument = null
            viewingDocAuthor = null
            viewingDocCourse = null
          },
          analysis = ViewerAnalysis(
            state = when (entry?.extractionStatus) {
              null -> AnalysisState.NONE
              DocumentAnalysisService.STATUS_COMPLETED -> AnalysisState.DONE
              DocumentAnalysisService.STATUS_PROCESSING -> AnalysisState.RUNNING
              else -> AnalysisState.FAILED
            },
            onAnalyse = {
              if (entry != null) {
                analysisService.retry(entry)
              } else {
                val code = viewingDocCourse?.takeIf { it.isNotBlank() } ?: "GENERAL"
                val titles = viewModel.catalogCourses.value[currentUser.universityId].orEmpty()
                analysisId = analysisService.enqueue(
                  DocumentAnalysisService.Request(
                    uri = Uri.parse(doc.uriString),
                    fileName = doc.fileName,
                    mimeType = doc.mimeType,
                    courseCode = code,
                    courseTitle = titles[code] ?: code,
                    university = currentUser.universityId,
                    uploaderName = currentUser.fullName,
                    category = "Notes"
                  )
                )
                Toast.makeText(appContext, viewModel.t("Analysing… the result will appear in AI Summaries.", "Inachambua… matokeo yataonekana kwenye Muhtasari wa AI."), Toast.LENGTH_LONG).show()
              }
            },
            onViewResult = {
              viewingDocument = null
              viewingDocAuthor = null
              viewingDocCourse = null
              currentTab = LecturerTab.PROCESSED_DOCS
            }
          ).takeIf { doc.uriString != null }
        )
      }
    }
  }
}
