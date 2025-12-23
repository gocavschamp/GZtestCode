plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.basetools"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
kapt {
    arguments {
        arg("AROUTER_MODULE_NAME", project.name)
    }
}
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
//    implementation(libs.easyphotos) {
//        exclude(group = "com.davemorrissey.labs", module = "subsampling-scale-image-view")
//    }
    api("com.github.bumptech.glide:glide:4.11.0") {
        exclude(group = "com.android.support")
    }
    implementation(project(":easyPhotos")){
        exclude(group = "com.davemorrissey.labs", module = "subsampling-scale-image-view")
    }
    kapt(libs.arouter.compiler)
    implementation(libs.arouter)
    annotationProcessor (libs.arouter.compiler)
    //检测内存泄漏
    //noinspection UseTomlInstead
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    releaseImplementation(libs.leakcanary.android.no.op)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // only support AndroidX
    implementation(libs.lphotopicker)
    implementation(libs.renderscrip.toolkit)

    api(libs.gift.lottie)

    api (libs.webpdecoder)
    api (libs.svga)
    // 导入RenderScript-toolkit。 也可以使用任何你编译的RenderScript-toolkit的库
//    implementation("io.github.limuyang2:renderscrip-toolkit:1.0.2")
}