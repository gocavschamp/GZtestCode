plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")

}

android {
    namespace = "com.ywm.baselibray"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        // 添加ARouter模块名称配置

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation(project(":baseapi"))

    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    kapt(libs.arouter.compiler)
    implementation(libs.arouter)
    implementation (libs.android.flexbox)
    annotationProcessor (libs.arouter.compiler)
    implementation(project(":basetools"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.tencent.tav:libpag:4.3.43")
// 使用最新版本
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
// 网络下载
    implementation("io.coil-kt:coil:2.4.0")
// 图片加载（可选，用于缓存管理）
}