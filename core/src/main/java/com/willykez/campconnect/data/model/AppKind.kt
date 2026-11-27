package com.willykez.campconnect.data.model

import com.willykez.campconnect.viewmodel.ScreenTab

enum class AppKind(
  val id: String,
  val title: String,
  val titleSw: String,
  val canRegister: Boolean,
  val registersStaff: Boolean,
  val tabs: List<ScreenTab>
) {
  OWNER("owner", "ChuoPulse Owner", "ChuoPulse Mmiliki", false, false, emptyList()),
  ADMIN(
    "admin", "ChuoPulse Admin", "ChuoPulse Msimamizi", false, false,
    listOf(ScreenTab.HOME, ScreenTab.TIMETABLE, ScreenTab.ADMIN, ScreenTab.CALENDAR, ScreenTab.PROFILE)
  ),
  LECTURER(
    "lecturer", "ChuoPulse Lecturer", "ChuoPulse Mhadhiri", true, true,
    listOf(ScreenTab.HOME, ScreenTab.TIMETABLE, ScreenTab.MATERIALS, ScreenTab.CALENDAR, ScreenTab.PROFILE)
  ),
  STUDENT(
    "student", "ChuoPulse Student", "ChuoPulse Mwanafunzi", true, false,
    listOf(ScreenTab.HOME, ScreenTab.TIMETABLE, ScreenTab.MATERIALS, ScreenTab.CALENDAR, ScreenTab.PROFILE)
  );

  val packageName: String get() = "com.willykez.campconnect.$id"

  fun accepts(profile: UserProfile): Boolean = when (this) {
    OWNER -> profile.isPlatformAdmin
    ADMIN -> profile.role == Role.ADMIN && !profile.isPlatformAdmin
    LECTURER -> profile.role == Role.LECTURER
    STUDENT -> profile.role == Role.STUDENT
  }

  companion object {
    fun of(profile: UserProfile): AppKind = when {
      profile.isPlatformAdmin -> OWNER
      profile.role == Role.ADMIN -> ADMIN
      profile.role == Role.LECTURER -> LECTURER
      else -> STUDENT
    }
  }
}
