import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Firebase (modo nube) se activa solo si existe app/google-services.json.
// Sin ese archivo la app funciona en modo local (datos guardados en el dispositivo).
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
}

// Inicio de sesión con Google: necesita el "ID de cliente web" (client_type 3) que Firebase añade
// a google-services.json al activar el proveedor Google y registrar la huella SHA-1 de la app.
// Si no está, el botón "Continuar con Google" no aparece. Se puede forzar con -Pmigasto.googleWebClientId=...
val googleWebClientId: String = providers.gradleProperty("migasto.googleWebClientId").orNull
    ?: providers.fileContents(layout.projectDirectory.file("google-services.json")).asText.orNull
        ?.let { text ->
            @Suppress("UNCHECKED_CAST")
            val json = groovy.json.JsonSlurper().parseText(text) as Map<String, Any?>
            val client = (json["client"] as? List<Map<String, Any?>>).orEmpty().firstOrNull {
                val info = it["client_info"] as? Map<String, Any?>
                val android = info?.get("android_client_info") as? Map<String, Any?>
                android?.get("package_name") == "com.chiiraac.migasto"
            }
            val services = client?.get("services") as? Map<String, Any?>
            val appInvite = services?.get("appinvite_service") as? Map<String, Any?>
            val oauthClients = (client?.get("oauth_client") as? List<Map<String, Any?>>).orEmpty() +
                (appInvite?.get("other_platform_oauth_client") as? List<Map<String, Any?>>).orEmpty()
            oauthClients.firstOrNull { it["client_type"]?.toString() == "3" }?.get("client_id")?.toString()
        }
    ?: ""
if (hasFirebaseConfig && googleWebClientId.isEmpty()) {
    logger.warn(
        "MiGasto: google-services.json no tiene ID de cliente web (client_type 3): " +
            "el botón «Continuar con Google» no aparecerá. Descarga de nuevo el archivo desde Firebase.",
    )
}

// Firma de release: keystore.properties en la raíz o variables de entorno (CI).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.chiiraac.migasto"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.chiiraac.migasto"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Apartado "Invítame a un café" (Bizum). Google Play puede rechazar enlaces de donación
        // que no usen su sistema de pagos: para la versión de Play se puede compilar con
        // ./gradlew bundleRelease -Pmigasto.bizum=false
        buildConfigField(
            "boolean",
            "SHOW_BIZUM",
            (project.findProperty("migasto.bizum")?.toString() ?: "true").toBoolean().toString(),
        )
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("storeFile", "MIGASTO_KEYSTORE_FILE")
            if (!storePath.isNullOrBlank()) {
                storeFile = rootProject.file(storePath)
                storePassword = signingValue("storePassword", "MIGASTO_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "MIGASTO_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "MIGASTO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                it.systemProperty("roborazzi.test.record", "true")
                // Los tests de la app completa usan siempre el modo local, aunque exista google-services.json
                it.systemProperty("migasto.forceLocal", "true")
                // Espejo de Maven Central para descargar los android-all de Robolectric
                it.systemProperty(
                    "robolectric.dependency.repo.url",
                    "https://maven-central.storage-download.googleapis.com/maven2",
                )
                it.maxHeapSize = "2g"
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
