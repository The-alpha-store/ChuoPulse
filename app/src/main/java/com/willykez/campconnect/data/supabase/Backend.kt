package com.willykez.campconnect.data.supabase

/** The one shared connection to Supabase, and the one answer to "is this build configured?". */
object Backend {
  val client: SupabaseClient by lazy { SupabaseClient() }
  val live: Boolean by lazy { client.isConfigured }
}
