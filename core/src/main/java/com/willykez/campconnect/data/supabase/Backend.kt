package com.willykez.campconnect.data.supabase

import android.content.Context
import com.willykez.campconnect.data.model.AppKind

object Backend {
  val client: SupabaseClient by lazy { SupabaseClient() }
  val live: Boolean by lazy { client.isConfigured }

  @Volatile var appContext: Context? = null
    private set

  @Volatile var appKind: AppKind = AppKind.STUDENT

  @Volatile var tokenSource: (() -> String?)? = null
  @Volatile var tokenRefresh: (suspend () -> String?)? = null

  fun init(context: Context) {
    appContext = context.applicationContext
  }
}
