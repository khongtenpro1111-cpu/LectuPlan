plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.tkyl.lectuplan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tkyl.lectuplan"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("LectuPlan.apk")
        }
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // 1. Thư viện vẽ biểu đồ theo Tuần / Học kỳ
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // 2. Thư viện đọc/xuất file Excel (.xlsx)
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // 3. Thư viện Google ML Kit Text Recognition (Đọc OCR chữ từ ảnh chụp bảng thông báo lịch tuần)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // 4. Thư viện đọc văn bản gốc chuẩn Unicode Tiếng Việt từ file PDF (PDFBox-Android)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    testImplementation("org.apache.pdfbox:pdfbox:2.0.27")
}
