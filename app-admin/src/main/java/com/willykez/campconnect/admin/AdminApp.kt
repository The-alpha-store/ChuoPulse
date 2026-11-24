@file:OptIn(ExperimentalMaterial3Api::class)
package com.willykez.campconnect.admin

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.willykez.campconnect.data.model.AccountStatus
import com.willykez.campconnect.data.model.Announcement
import com.willykez.campconnect.data.model.AppLanguage
import com.willykez.campconnect.data.model.AttachmentFile
import com.willykez.campconnect.data.model.Role
import com.willykez.campconnect.ui.components.AnnouncementDetailDialog
import com.willykez.campconnect.ui.components.CancelSessionDialog
import com.willykez.campconnect.ui.components.ChangeVenueDialog
import com.willykez.campconnect.ui.components.DocumentViewerDialog
import com.willykez.campconnect.ui.components.EditAnnouncementDialog
import com.willykez.campconnect.ui.components.PublishAnnouncementDialog
import com.willykez.campconnect.ui.screens.AdminScreen
import com.willykez.campconnect.ui.screens.AuthGate
import com.willykez.campconnect.ui.screens.HomeScreen
import com.willykez.campconnect.ui.screens.MaintenanceScreen
import com.willykez.campconnect.ui.screens.NotificationsScreen
import com.willykez.campconnect.ui.screens.ProfileScreen
import com.willykez.campconnect.ui.screens.SplashScreen
import com.willykez.campconnect.ui.screens.TimetableScreen
import com.willykez.campconnect.ui.screens.WaitingRoomScreen
import com.willykez.campconnect.ui.screens.WrongAppScreen
import com.willykez.campconnect.ui.theme.AppThemeSettings
import com.willykez.campconnect.ui.theme.CampusConnectTheme
import com.willykez.campconnect.ui.theme.DangerRed
import com.willykez.campconnect.ui.theme.DeepBlue
import com.willykez.campconnect.ui.theme.WarmAmber
import com.willykez.campconnect.viewmodel.CampusViewModel

private enum class AdminTab {
  ADMIN,
  TIMETABLE,
  NOTICES,
  NOTIFICATIONS,
  PROFILE
}

/**
 * Root composable for the Admin app (`com.willykez.campconnect.admin`).
 * Contains screens strictly tailored to campus administrators:
 * - Admin Control Center (approvals, people, academic structure, logs)
 * - Timetable & Session oversight
 * - Campus announcements & moderation
 * - Profile & Maintenance controls
 */
@Composable
fun AdminApp(viewModel: CampusViewModel = viewModel()) {
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
      // Dedicated Role guard: Campus Admins only (Platform admins have Owner app)
      user.isPlatformAdmin || user.role != Role.ADMIN -> WrongAppScreen(
        viewModel = viewModel,
        expectedRoleLabel = viewModel.t("Campus Admin", "Msimamizi wa Chuo")
      )
      paused -> MaintenanceScreen(viewModel)
      else -> AdminMainContent(viewModel)
    }
  }
}

@Composable
private fun AdminMainContent(viewModel: CampusViewModel) {
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

  var currentTab by rememberSaveable { mutableStateOf(AdminTab.ADMIN) }
  BackHandler(enabled = currentTab != AdminTab.ADMIN) { currentTab = AdminTab.ADMIN }

  val currentUser by viewModel.currentUser.collectAsState()
  val isOffline by viewModel.isOffline.collectAsState()
  val syncing by viewModel.syncing.collectAsState()
  val unreadNotifs by viewModel.unreadNotificationsCount.collectAsState()
  val pendingUsers by viewModel.pendingUsers.collectAsState()
  val announcements by viewModel.announcements.collectAsState()
  val sessions by viewModel.sessions.collectAsState()
  val remindedIds by viewModel.remindedAnnouncements.collectAsState()

  var selectedAnnouncementId by remember { mutableStateOf<String?>(null) }
  var showPublishDialog by remember { mutableStateOf(false) }
  var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
  var venueChangeSessionId by remember { mutableStateOf<String?>(null) }
  var cancelSessionId by remember { mutableStateOf<String?>(null) }
  var viewingDocument by remember { mutableStateOf<AttachmentFile?>(null) }
  var viewingDocAuthor by remember { mutableStateOf<String?>(null) }
  var viewingDocCourse by remember { mutableStateOf<String?>(null) }

  Scaffold(
    modifier = Modifier.fillMaxSize().testTag("admin_main_scaffold"),
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
                  text = "ChuoPulse Admin",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFDCFCE7))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "ADMIN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF166534),
                    fontSize = 9.sp
                  )
                }
              }
              Text(
                text = if (isOffline) viewModel.getString("offline_notice")
                else if (syncing) viewModel.t("Syncing…", "Inasawazisha…")
                else "${currentUser.fullName.split(" ").firstOrNull() ?: ""} • Campus Control",
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

          // Unread notifications
          IconButton(onClick = { currentTab = AdminTab.NOTIFICATIONS }, modifier = Modifier.testTag("admin_top_notif_btn")) {
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
                tint = if (currentTab == AdminTab.NOTIFICATIONS) DeepBlue else MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // Profile / Menu
          Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("admin_top_menu_btn")) {
              BadgedBox(
                badge = {
                  if (pendingUsers.isNotEmpty()) {
                    Badge(containerColor = WarmAmber, contentColor = Color.White) { Text("${pendingUsers.size}") }
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
                text = { Text(viewModel.t("Admin Profile", "Wasifu wa Msimamizi")) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                onClick = { menuOpen = false; currentTab = AdminTab.PROFILE }
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
          selected = currentTab == AdminTab.ADMIN,
          onClick = { currentTab = AdminTab.ADMIN },
          icon = {
            BadgedBox(
              badge = {
                if (pendingUsers.isNotEmpty()) {
                  Badge(containerColor = WarmAmber, contentColor = Color.White) { Text("${pendingUsers.size}") }
                }
              }
            ) {
              Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin")
            }
          },
          label = { Text(viewModel.t("Control", "Kudhibiti")) },
          colors = NavigationBarItemDefaults.colors(indicatorColor = DeepBlue.copy(alpha = 0.15f))
        )

        NavigationBarItem(
          selected = currentTab == AdminTab.TIMETABLE,
          onClick = { currentTab = AdminTab.TIMETABLE },
          icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Timetable") },
          label = { Text(viewModel.getString("tab_timetable")) },
          colors = NavigationBarItemDefaults.colors(indicatorColor = DeepBlue.copy(alpha = 0.15f))
        )

        NavigationBarItem(
          selected = currentTab == AdminTab.NOTICES,
          onClick = { currentTab = AdminTab.NOTICES },
          icon = { Icon(Icons.Default.Campaign, contentDescription = "Notices") },
          label = { Text(viewModel.t("Notices", "Matangazo")) },
          colors = NavigationBarItemDefaults.colors(indicatorColor = DeepBlue.copy(alpha = 0.15f))
        )

        NavigationBarItem(
          selected = currentTab == AdminTab.PROFILE,
          onClick = { currentTab = AdminTab.PROFILE },
          icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
          label = { Text(viewModel.getString("tab_profile")) },
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
      Crossfade(targetState = currentTab, animationSpec = tween(180), label = "admin_tab") { tab ->
        when (tab) {
          AdminTab.ADMIN -> AdminScreen(viewModel = viewModel)
          AdminTab.TIMETABLE -> TimetableScreen(
            viewModel = viewModel,
            onOpenChangeVenueDialog = { venueChangeSessionId = it },
            onOpenCancelDialog = { cancelSessionId = it }
          )
          AdminTab.NOTICES -> HomeScreen(
            viewModel = viewModel,
            onNavigateToNotifications = { currentTab = AdminTab.NOTIFICATIONS },
            onOpenAnnouncementDetail = { selectedAnnouncementId = it },
            onOpenPublishDialog = { showPublishDialog = true },
            onOpenChangeVenueDialog = { venueChangeSessionId = it },
            onOpenUploadDialog = { /* Admins manage structure, not individual course notes */ },
            onOpenAddDeadlineDialog = { /* Admins do not schedule assignments */ }
          )
          AdminTab.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
          AdminTab.PROFILE -> ProfileScreen(viewModel = viewModel)
        }
      }

      // Dialogs for Admin
      selectedAnnouncementId?.let { id ->
        val announcement = announcements.find { it.id == id }
        if (announcement != null) {
          AnnouncementDetailDialog(
            announcement = announcement,
            currentUserRole = Role.ADMIN,
            canModerate = true,
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
            onToggleApproval = { status ->
              viewModel.setAnnouncementApproval(announcement.id, status)
            },
            showReadStats = true,
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
        DocumentViewerDialog(
          attachment = doc,
          authorOrUploaderName = viewingDocAuthor,
          courseCode = viewingDocCourse,
          onDismiss = {
            viewingDocument = null
            viewingDocAuthor = null
            viewingDocCourse = null
          }
        )
      }
    }
  }
}
