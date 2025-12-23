import org.gradle.kotlin.dsl.annotationProcessor

plugins {
    id("kotlin-kapt")
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.homelogic"
    compileSdk = 36
    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        // 添加ARouter模块名称配置
        kapt {
            arguments {
                arg("AROUTER_MODULE_NAME", project.name)
            }
        }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding {
        enable = true
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    kapt(libs.arouter.compiler)
    implementation(libs.arouter)
    annotationProcessor (libs.arouter.compiler)
//    annotationProcessor "com.github.JailedBird:ArouterKspCompiler:1.8.20-1.0.7"
//    kapt "com.github.JailedBird:ArouterKspCompiler:1.8.20-1.0.7"
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}