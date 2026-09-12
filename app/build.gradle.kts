plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// ── Release 签名配置 ────────────────────────────────────────────────────────
// CI 会把 Secrets 里的 keystore 解码到 $RUNNER_TEMP/release.jks，并把绝对路径
// 写进环境变量 RELEASE_KEYSTORE_PATH。本地开发可以自己放一份到
// keystore/release.jks（.gitignore 已经忽略 *.jks，不会被提交）。
// 两个位置都没有这个文件时，回退成 debug 签名 —— 保证刚克隆下来的仓库
// 直接跑 `./gradlew assembleRelease` 依然能成功，不会因为缺密钥而失败。
val releaseKeystoreFile = System.getenv("RELEASE_KEYSTORE_PATH")?.let { file(it) }
    ?: rootProject.file("keystore/release.jks")

android {
    namespace = "com.gcap.client"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gcap.client"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        if (releaseKeystoreFile.exists()) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseKeystoreFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                // 没有正式 keystore：退回 debug 签名，产物仍可安装（但不能上架）
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    
    // Navigation & Lifecycle
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // Network
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    
    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // DataStore
    implementation(libs.datastore.preferences)
    
    // Security
    implementation(libs.security.crypto)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Markdown
    implementation(libs.markdown.renderer.m3)
    implementation(libs.markdown.renderer.coil3)

    // Testing
    testImplementation(libs.junit)
}
