package com.willykez.campconnect.student

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.willykez.campconnect.data.model.AppKind
import com.willykez.campconnect.data.supabase.Backend
import com.willykez.campconnect.ui.CampusConnectApp
import com.willykez.campconnect.ui.theme.AppThemeSettings

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Backend.init(this)
    Backend.appKind = AppKind.STUDENT
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
    )
    AppThemeSettings.load(this)
    setContent {
      CampusConnectApp()
    }
  }
}
