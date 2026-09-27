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

        // ── FASE 2 · Cloudinary (subida de fotos) ────────────────────────────
        // Credenciales PÚBLICAS: cloud name + preset sin firmar.
        // El "API Secret" se queda fuera de la app a propósito; vive solo en el
        // dashboard de Cloudinary.
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"vtmk2tgh\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"chambaya_preset\"")
        // Raíz de los módulos de imagen. Cada tipo de imagen va a su propio módulo
        // para que el dashboard de Cloudinary no mezcle avatares con mapas o con
        // anuncios:
        //
        //   chambaya/
        //     oficios/            <- imágenes de las categorías de oficios
        //                              (las sube quien mantiene api_oficios.json)
        //     fotos-perfil/       <- avatares, una carpeta por uid
        //     fotos-lugares/      <- fotos de los lugares del mapa
        //     fotos-publicaciones/<- fotos de los anuncios y publicaciones
        //
        // Sin espacios ni acentos a propósito: estas rutas acaban dentro de una
        // expresión regular de `firestore.rules` y en la URL pública, y un espacio
        // obligaría a escribir `%20` en los dos sitios.
        buildConfigField("String", "CLOUDINARY_FOLDER_ROOT", "\"chambaya\"")
        buildConfigField("String", "CLOUDINARY_FOLDER_PERFILES", "\"fotos-perfil\"")
        buildConfigField("String", "CLOUDINARY_FOLDER_LUGARES", "\"fotos-lugares\"")
        buildConfigField("String", "CLOUDINARY_FOLDER_PUBLICACIONES", "\"fotos-publicaciones\"")

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