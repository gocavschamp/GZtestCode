plugins {
    id("kotlin-kapt")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

import java.util.Properties
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 构建日期（用于 APK 文件名）
val buildDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

// 从根目录 keystore.properties 读取签名信息（该文件不进版本库）
val keystoreProperties = Properties().apply {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) {
        FileInputStream(propsFile).use { load(it) }
    }
}

android {
    namespace = "com.example.firstapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.firstapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 添加ARouter模块名称配置

    }

    // 签名配置（keystore 位于 app/kidgarden.jks，密码从根目录 keystore.properties 读取）
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile") ?: "kidgarden.jks")
            storePassword = keystoreProperties.getProperty("storePassword") ?: "yu123456"
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: "kidgarden"
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: "yu123456"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // APK 文件名带版本和构建日期：kidgarden_1.0_20260808_debug.apk / kidgarden_1.0_20260808_release.apk
    applicationVariants.all {
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl.outputFileName = "kidgarden_${versionName}_${buildDate}_${name}.apk"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    viewBinding {
        enable = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}
kapt {
    arguments {
        arg("AROUTER_MODULE_NAME", project.name)
    }
}
dependencies {
    implementation(libs.kotlin.stdlib.jdk7)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(project(":homelogic"))
    implementation(project(":baseapi"))
    implementation(project(":basetools")){
        exclude(group = "com.davemorrissey.labs", module = "subsampling-scale-image-view")
    }
    implementation(project(":baselibray"))
    implementation(project(":animplayer"))
    kapt(libs.arouter.compiler)
    implementation(libs.arouter)
    annotationProcessor (libs.arouter.compiler)
    implementation (libs.android.flexbox)
    implementation(libs.skeleton)
    implementation(libs.shimmerlayout)
    // Room 数据库（幼儿教育模块本地存储）
    implementation("androidx.room:room-runtime:${libs.versions.room.get()}")
    implementation("androidx.room:room-ktx:${libs.versions.room.get()}")
    kapt("androidx.room:room-compiler:${libs.versions.room.get()}")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    configurations.all {
        exclude(group = "com.davemorrissey.labs", module = "subsampling-scale-image-view")
    }
//    implementation(libs.vap)
}