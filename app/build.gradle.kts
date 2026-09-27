plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.proyecto.chambaya"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.proyecto.chambaya"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // ── FASE 2 · Cloudinary (subida de foto de perfil) ──────────────────
        // Credenciales PÚBLICAS: cloud name + preset sin firmar.
        // El "API Secret" (W3mN_1hUVkUbkaU-s1Dhaezx25Q) se queda fuera de la
        // app a propósito; vive solo en el dashboard de Cloudinary.
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"vtmk2tgh\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"chambaya_unsigned\"")
        // Carpeta raíz de fotos de perfil dentro de Cloudinary.
        buildConfigField("String", "CLOUDINARY_PROFILE_FOLDER", "\"chambaya/perfiles\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
        // FASE 2: `BuildConfig` para las credenciales públicas de Cloudinary.
        // Solo van el Cloud Name y el nombre del preset unsigned: el API Secret
        // NUNCA debe entrar en la app (permitiría subir/borrar desde el móvil).
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Firebase & Auth
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Compose
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    // Splash screen
    implementation(libs.androidx.core.splashscreen)

    // MapLibre Maps SDK (Native Mapbox Vector Rendering Engine) & Location Services
    implementation("org.maplibre.gl:android-sdk:11.8.3")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Coil for SVG support (mejor que Glide para SVG)
    implementation("io.coil-kt:coil:2.5.0")
    implementation("io.coil-kt:coil-svg:2.5.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}