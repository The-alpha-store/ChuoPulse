import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// One opt-in for the whole module, so @OptIn(ExperimentalMaterial3Api::class) is not needed anywhere.
kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }
}

// ---- Build settings: Gradle property > environment variable (CI) > .env file > default ----
// The Secrets Gradle Plugin used to read the .env file as well, and its placeholder values could win
// over the real ones, silently leaving the app unconfigured. These keys are now ignored by that plugin
// (see the `secrets` block) and resolved only here, in one place.
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

fun isPlaceholder(value: String) = value.isBlank() || value.contains("YOUR_", ignoreCase = true)

android {
  namespace = "com.willykez.campconnect"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.willykez.campconnect"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

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

  signingConfigs {
    val releaseKeystorePath = System.getenv("KEYSTORE_PATH")
    val releaseStorePassword = System.getenv("STORE_PASSWORD")
    val releaseKeyPassword = System.getenv("KEY_PASSWORD")

    if (!releaseKeystorePath.isNullOrBlank() &&
      !releaseStorePassword.isNullOrBlank() &&
      !releaseKeyPassword.isNullOrBlank()
    ) {
      create("release") {
        storeFile = file(releaseKeystorePath)
        storePassword = releaseStorePassword
        keyAlias = "upload"
        keyPassword = releaseKeyPassword
      }
    }

    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )

      if (signingConfigs.findByName("release") != null) {
        signingConfig = signingConfigs.getByName("release")
      }
    }

    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
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

  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  // Resolved by setting(...) above so the placeholder values in .env.example can never win.
  ignoreList.add("SUPABASE_URL")
  ignoreList.add("SUPABASE_ANON_KEY")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.mlkit.text.recognition)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)

  debugImplementation(libs.androidx.compose.ui.tooling)

  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// A release build must never ship without a real backend.
tasks.register("verifyLiveConfig") {
  doLast {
    val url = setting("SUPABASE_URL")
    val key = setting("SUPABASE_ANON_KEY")
    if (isPlaceholder(url) || isPlaceholder(key)) {
      throw GradleException(
        "SUPABASE_URL and SUPABASE_ANON_KEY are missing or still placeholders. " +
          "Set them in .env, or as GitHub repository secrets, before building a release. See README.md."
      )
    }
  }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn("verifyLiveConfig") }
