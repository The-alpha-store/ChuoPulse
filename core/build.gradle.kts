plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }
}

val dotEnv: Map<String, String> = rootProject.file(".env").takeIf { it.exists() }
  ?.readLines()
  ?.map { it.trim() }
  ?.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
  ?.associate { it.substringBefore("=").trim() to it.substringAfter("=").trim().trim('"', '\'') }
  ?: emptyMap()

fun setting(name: String, default: String = ""): String =
  providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orNull
    ?: dotEnv[name]
    ?: default

android {
  namespace = "com.willykez.campconnect"
  compileSdk = 36

  defaultConfig {
    minSdk = 24
    consumerProguardFiles("consumer-rules.pro")

    val supabaseUrl = setting("SUPABASE_URL")
    val supabaseAnonKey = setting("SUPABASE_ANON_KEY")

    buildConfigField(
      "String",
      "SUPABASE_URL",
      "\"${supabaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    )

    buildConfigField(
      "String",
      "SUPABASE_ANON_KEY",
      "\"${supabaseAnonKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    )
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  lint {
    abortOnError = false
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.core.ktx)
  implementation(libs.mlkit.text.recognition)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.okhttp)

  "ksp"(libs.androidx.room.compiler)
}

val liveUrlProvider = providers.gradleProperty("SUPABASE_URL")
  .orElse(providers.environmentVariable("SUPABASE_URL"))
  .orElse(provider { dotEnv["SUPABASE_URL"] ?: "" })

val liveKeyProvider = providers.gradleProperty("SUPABASE_ANON_KEY")
  .orElse(providers.environmentVariable("SUPABASE_ANON_KEY"))
  .orElse(provider { dotEnv["SUPABASE_ANON_KEY"] ?: "" })

tasks.register("verifyLiveConfig") {
  val urlProvider = liveUrlProvider
  val keyProvider = liveKeyProvider
  inputs.property("supabaseUrl", urlProvider)
  inputs.property("supabaseAnonKey", keyProvider)

  doLast {
    val url = urlProvider.get()
    val key = keyProvider.get()
    val isInvalidUrl = url.isBlank() || url.contains("YOUR_", ignoreCase = true)
    val isInvalidKey = key.isBlank() || key.contains("YOUR_", ignoreCase = true)

    if (isInvalidUrl || isInvalidKey) {
      throw GradleException(
        "SUPABASE_URL and SUPABASE_ANON_KEY are missing or still placeholders. " +
          "Set them in .env, or as GitHub repository secrets, before building a release. See README.md."
      )
    }
  }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn("verifyLiveConfig") }
