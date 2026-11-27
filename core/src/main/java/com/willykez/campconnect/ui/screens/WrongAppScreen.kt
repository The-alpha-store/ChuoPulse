package com.willykez.campconnect.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.willykez.campconnect.data.model.AppKind
import com.willykez.campconnect.data.model.UserProfile
import com.willykez.campconnect.viewmodel.CampusViewModel

@Composable
fun WrongAppScreen(viewModel: CampusViewModel, thisApp: AppKind, user: UserProfile) {
  val context = LocalContext.current
  val right = AppKind.of(user)
  var missing by remember { mutableStateOf(false) }
  val rightName = viewModel.t(right.title, right.titleSw)

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      modifier = Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = viewModel.t("Signed in, but this is the wrong app", "Umeingia, lakini hii si programu sahihi"),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("wrong_app_title")
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = viewModel.t(
          "${user.fullName}, your account works in $rightName. This app (${thisApp.title}) is for a different kind of account, so nothing is loaded here.",
          "${user.fullName}, akaunti yako inafanya kazi kwenye $rightName. Programu hii (${thisApp.titleSw}) ni ya akaunti ya aina nyingine, kwa hiyo hakuna kinachopakiwa hapa."
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = {
          val launch = context.packageManager.getLaunchIntentForPackage(right.packageName)
          if (launch == null) missing = true else context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        modifier = Modifier.fillMaxWidth().testTag("wrong_app_open")
      ) { Text(viewModel.t("Open $rightName", "Fungua $rightName")) }
      if (missing) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = viewModel.t(
            "$rightName is not installed on this phone. Install it, then sign in with the same email and password.",
            "$rightName haijasakinishwa kwenye simu hii. Isakinishe, kisha ingia kwa barua pepe na nenosiri lilelile."
          ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          textAlign = TextAlign.Center
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedButton(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth().testTag("wrong_app_sign_out")) {
        Text(viewModel.t("Sign out", "Toka"))
      }
    }
  }
}
