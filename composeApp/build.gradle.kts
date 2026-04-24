import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val generatedSupabaseConfigDir = layout.buildDirectory.dir("generated/supabaseConfig/commonMain/kotlin")

val generateSupabaseConfig by tasks.registering {
    val supabaseEnvironment = providers.gradleProperty("supabase.env")
        .orElse(providers.environmentVariable("SUPABASE_ENV"))
        .orElse("local")
    val localUrl = providers.gradleProperty("supabase.local.url")
        .orElse(providers.environmentVariable("SUPABASE_LOCAL_URL"))
        .orElse("http://127.0.0.1:54321")
    val localKey = providers.gradleProperty("supabase.local.key")
        .orElse(providers.environmentVariable("SUPABASE_LOCAL_KEY"))
        .orElse("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0")
    val prodUrl = providers.gradleProperty("supabase.prod.url")
        .orElse(providers.environmentVariable("SUPABASE_PROD_URL"))
        .orElse("https://nnhvnqpczerqrofxkbki.supabase.co")
    val prodKey = providers.gradleProperty("supabase.prod.key")
        .orElse(providers.environmentVariable("SUPABASE_PROD_KEY"))
        .orElse("sb_publishable_NyBL91k2rrDYgnpKWvYhGA_cXhLoVAq")

    inputs.property("supabaseEnvironment", supabaseEnvironment)
    inputs.property("localUrl", localUrl)
    inputs.property("localKey", localKey)
    inputs.property("prodUrl", prodUrl)
    inputs.property("prodKey", prodKey)
    outputs.dir(generatedSupabaseConfigDir)

    doLast {
        val environment = supabaseEnvironment.get().lowercase()
        val config = when (environment) {
            "local", "dev", "development" -> Triple("local", localUrl.get(), localKey.get())
            "prod", "production" -> Triple("prod", prodUrl.get(), prodKey.get())
            else -> error("Unknown Supabase environment '$environment'. Use local or prod.")
        }

        fun String.toKotlinStringLiteral() = buildString {
            append('"')
            this@toKotlinStringLiteral.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }

        val outputFile = generatedSupabaseConfigDir.get()
            .file("mok/it/tortura/SupabaseConfig.kt")
            .asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package mok.it.tortura

            internal object SupabaseConfig {
                const val ENVIRONMENT = ${config.first.toKotlinStringLiteral()}
                const val URL = ${config.second.toKotlinStringLiteral()}
                const val KEY = ${config.third.toKotlinStringLiteral()}
            }
            """.trimIndent() + "\n",
        )
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
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

        commonMain {
            kotlin.srcDir(generatedSupabaseConfigDir)
        }
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

            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateSupabaseConfig)
}

compose.desktop {
    application {
        mainClass = "mok.it.tortura.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Tortura"
            packageVersion = "1.0.0"

            linux {
                modules("jdk.security.auth") // needed to access file system
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }

            windows {
                shortcut = true
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
            }
        }
    }
}
