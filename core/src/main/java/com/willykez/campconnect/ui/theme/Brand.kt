package com.willykez.campconnect.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.willykez.campconnect.data.model.AppKind
import com.willykez.campconnect.data.supabase.Backend

/**
 * One palette per app, matching its launcher icon:
 * Student = blue, Admin = green, Lecturer = amber, Owner = slate/navy.
 *
 * Every screen reads the brand through [BrandPrimary], [BrandDeep] and [BrandLight], so changing a palette here
 * recolours the whole app. Light and dark themes keep working: the page colours (background, surfaces, text) stay
 * shared, only the accent changes.
 */
class AppPalette(
  /** Accent in the light theme: buttons, selected chips, icons, links. White text must stay readable on it. */
  val accentLight: Color,
  /** Accent in the dark theme: slightly lighter where the app's accent would vanish on a dark surface. */
  val accentDark: Color,
  /** Darkest shade, start of hero gradients. */
  val deep: Color,
  /** Lightest shade, end of hero gradients. */
  val light: Color,
  // Material colour scheme
  val primaryOnDark: Color,
  val onPrimaryOnDark: Color,
  val containerLight: Color,
  val onContainerLight: Color,
  val containerDark: Color,
  val onContainerDark: Color
)

object AppPalettes {
  val Student = AppPalette(
    accentLight = Color(0xFF1F4FD8), accentDark = Color(0xFF1F4FD8),
    deep = Color(0xFF123499), light = Color(0xFF3B6DF6),
    primaryOnDark = Color(0xFF8FA8FF), onPrimaryOnDark = Color(0xFF001E6C),
    containerLight = Color(0xFFDDE3FF), onContainerLight = Color(0xFF001453),
    containerDark = Color(0xFF123499), onContainerDark = Color(0xFFDAE2FF)
  )
  val Admin = AppPalette(
    accentLight = Color(0xFF15803D), accentDark = Color(0xFF16A34A),
    deep = Color(0xFF14532D), light = Color(0xFF22A455),
    primaryOnDark = Color(0xFF86EFAC), onPrimaryOnDark = Color(0xFF053D1C),
    containerLight = Color(0xFFDCFCE7), onContainerLight = Color(0xFF052E16),
    containerDark = Color(0xFF14532D), onContainerDark = Color(0xFFBBF7D0)
  )
  val Lecturer = AppPalette(
    accentLight = Color(0xFFB45309), accentDark = Color(0xFFD97706),
    deep = Color(0xFF78350F), light = Color(0xFFD97706),
    primaryOnDark = Color(0xFFFBBF24), onPrimaryOnDark = Color(0xFF451A03),
    containerLight = Color(0xFFFEF3C7), onContainerLight = Color(0xFF451A03),
    containerDark = Color(0xFF92400E), onContainerDark = Color(0xFFFDE68A)
  )
  val Owner = AppPalette(
    accentLight = Color(0xFF334155), accentDark = Color(0xFF64748B),
    deep = Color(0xFF0F172A), light = Color(0xFF475569),
    primaryOnDark = Color(0xFFCBD5E1), onPrimaryOnDark = Color(0xFF0F172A),
    containerLight = Color(0xFFE2E8F0), onContainerLight = Color(0xFF0F172A),
    containerDark = Color(0xFF1E293B), onContainerDark = Color(0xFFE2E8F0)
  )

  fun of(kind: AppKind): AppPalette = when (kind) {
    AppKind.STUDENT -> Student
    AppKind.ADMIN -> Admin
    AppKind.LECTURER -> Lecturer
    AppKind.OWNER -> Owner
  }
}

object Brand {
  /** Set by [CampusConnectTheme] so the accent can follow the light/dark theme. */
  var dark by mutableStateOf(false)

  val palette: AppPalette get() = AppPalettes.of(Backend.appKind)
}

/** Main accent of the running app (buttons, selected chips, icons, links). */
val BrandPrimary: Color get() = if (Brand.dark) Brand.palette.accentDark else Brand.palette.accentLight

/** Darkest brand shade: start of hero gradients. */
val BrandDeep: Color get() = Brand.palette.deep

/** Lightest brand shade: end of hero gradients. */
val BrandLight: Color get() = Brand.palette.light
