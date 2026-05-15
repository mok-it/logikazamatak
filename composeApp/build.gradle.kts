import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.*
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun loadDotEnv(fileName: String): Map<String, String> {
    val file = rootProject.file(fileName)
    if (!file.exists()) return emptyMap()

    val properties = Properties()
    file.inputStream().use(properties::load)
    return properties.stringPropertyNames().associateWith { key -> properties.getProperty(key) }
}

fun resolveSetting(
    gradlePropertyName: String,
    environmentVariableName: String,
    fallback: String? = null,
): String? = providers.gradleProperty(gradlePropertyName).orNull
    ?: providers.environmentVariable(environmentVariableName).orNull
    ?: fallback

fun requireSetting(
    gradlePropertyName: String,
    environmentVariableName: String,
    fallback: String? = null,
): String = resolveSetting(
    gradlePropertyName = gradlePropertyName,
    environmentVariableName = environmentVariableName,
    fallback = fallback,
) ?: error(
    "Missing required build setting '$environmentVariableName' (or Gradle property '$gradlePropertyName'). " +
        "Set it in your environment, pass -P$gradlePropertyName=..., or put it in the appropriate .env file.",
)

val dotEnvLocal = loadDotEnv(".env.local")
val dotEnvProduction = loadDotEnv(".env.production")

val supabaseEnvironment = requireSetting(
    gradlePropertyName = "supabase.env",
    environmentVariableName = "SUPABASE_ENV",
    fallback = dotEnvLocal["SUPABASE_ENV"] ?: dotEnvProduction["SUPABASE_ENV"],
)

val activeDotEnv = when (supabaseEnvironment.lowercase()) {
    "local", "dev", "development" -> dotEnvLocal
    "prod", "production" -> dotEnvProduction
    else -> error("Unknown Supabase environment '$supabaseEnvironment'. Use local or prod.")
}

val supabaseUrl = requireSetting(
    gradlePropertyName = "supabase.url",
    environmentVariableName = "SUPABASE_URL",
    fallback = activeDotEnv["SUPABASE_URL"],
)

val supabaseKey = requireSetting(
    gradlePropertyName = "supabase.key",
    environmentVariableName = "SUPABASE_KEY",
    fallback = activeDotEnv["SUPABASE_KEY"],
)

val batkabankApiBaseUrl = requireSetting(
    gradlePropertyName = "batkabank.apiBaseUrl",
    environmentVariableName = "BATKABANK_API_BASE_URL",
    fallback = activeDotEnv["BATKABANK_API_BASE_URL"],
)

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

buildkonfig {
    packageName = "mok.it.tortura"
    objectName = "AppConfig"

    defaultConfigs {
        buildConfigField(STRING, "SUPABASE_ENVIRONMENT", supabaseEnvironment)
        buildConfigField(STRING, "SUPABASE_URL", supabaseUrl)
        buildConfigField(STRING, "SUPABASE_KEY", supabaseKey)
        buildConfigField(STRING, "BATKABANK_API_BASE_URL", batkabankApiBaseUrl)
    }
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting
        val jsMain by getting
        val wasmJsMain by getting

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.compose.navigation)
            implementation(libs.materialIconsExtended)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)

            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
            implementation(libs.slf4j.simple)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

android {
    namespace = "mok.it.tortura"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "mok.it.tortura"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "mok.it.tortura.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Tortura"
            packageVersion = "1.0.0"

            linux {
                modules("jdk.security.auth")
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }

            windows {
                shortcut = true
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
            }
        }
    }
}
