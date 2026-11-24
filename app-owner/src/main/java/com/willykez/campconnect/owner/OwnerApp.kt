package com.willykez.campconnect.owner

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willykez.campconnect.data.model.AccountStatus
import com.willykez.campconnect.data.model.AppLanguage
import com.willykez.campconnect.ui.screens.AuthGate
import com.willykez.campconnect.ui.screens.PlatformScreen
import com.willykez.campconnect.ui.screens.SplashScreen
import com.willykez.campconnect.ui.screens.WaitingRoomScreen
import com.willykez.campconnect.ui.screens.WrongAppScreen
import com.willykez.campconnect.ui.theme.CampusConnectTheme
import com.willykez.campconnect.viewmodel.CampusViewModel

/**
 * Root composable for the Owner (Platform Manager) app.
 *
 * Access rule: Only accounts with [isPlatformAdmin == true] reach [PlatformScreen].
 * Anyone else — including campus Admins, Lecturers and Students — lands on [WrongAppScreen].
 */
@Composable
fun OwnerApp(viewModel: CampusViewModel = viewModel()) {
  val session by viewModel.session.collectAsState()
  val user by viewModel.currentUser.collectAsState()
  val restoring by viewModel.restoring.collectAsState()
  val swahili by viewModel.appLanguage.collectAsState()
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
      // ✅ Role guard — this app is exclusively for the Platform Manager
      !user.isPlatformAdmin -> WrongAppScreen(
        viewModel = viewModel,
        expectedRoleLabel = viewModel.t("Owner / Platform Manager", "Msimamizi wa Mfumo")
      )
      else -> PlatformScreen(viewModel)
    }
  }
}
