package com.willykez.campconnect.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector
import com.willykez.campconnect.data.model.AppKind

/** What each app calls itself on the splash, sign-in and top bar. */
class AppIdentity(
  val icon: ImageVector,
  val roleEn: String,
  val roleSw: String,
  val taglineEn: String,
  val taglineSw: String
)

fun AppKind.identity(): AppIdentity = when (this) {
  AppKind.STUDENT -> AppIdentity(
    Icons.Default.School, "Student", "Mwanafunzi",
    "Your timetable, notices and deadlines in one place.",
    "Ratiba, matangazo na tarehe za mwisho mahali pamoja."
  )
  AppKind.LECTURER -> AppIdentity(
    Icons.Default.Groups, "Lecturer", "Mhadhiri",
    "Your classes, materials and student work in one place.",
    "Vipindi, nyenzo na kazi za wanafunzi mahali pamoja."
  )
  AppKind.ADMIN -> AppIdentity(
    Icons.Default.Business, "Campus administrator", "Msimamizi wa chuo",
    "Run your campus: people, timetable and notices.",
    "Simamia chuo chako: watu, ratiba na matangazo."
  )
  AppKind.OWNER -> AppIdentity(
    Icons.Default.AdminPanelSettings, "Platform owner", "Mmiliki wa jukwaa",
    "Manage every campus on ChuoPulse.",
    "Simamia vyuo vyote vya ChuoPulse."
  )
}
