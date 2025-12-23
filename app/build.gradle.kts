plugins {
    id("kotlin-kapt")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    configurations.all {
        exclude(group = "com.davemorrissey.labs", module = "subsampling-scale-image-view")
    }
//    implementation(libs.vap)
}