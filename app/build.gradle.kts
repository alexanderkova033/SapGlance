import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

// keystore/keystore.properties is gitignored (contains real passwords) and won't exist on
// CI or a fresh checkout; release signing is applied only when it's present, so CI's release
// build simply stays unsigned rather than failing.
val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            load(keystorePropertiesFile.inputStream())
        }
    }

android {
    namespace = "com.sapglance.app"
    // minSdk 26 gives us java.time natively (no desugaring needed by TipEngine)
    // and Glance's minimum supported API level.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sapglance.app"
        minSdk = 26
        targetSdk = 36
        // versionCode is what Play orders uploads by and must increase with every upload;
        // versionName is only ever read by humans. 1 is correct for a first upload.
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file("keystore/${keystoreProperties["storeFile"]}")
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Ship every language in the base APK instead of letting Play split them by device locale.
    //
    // Play's default is to deliver only the locales the device is set to, which is the right
    // trade for most apps and the wrong one for this one: SapGlance has an in-app language
    // picker, so a reader on an English phone can choose Russian and would find the Russian
    // strings had never been installed. The tips themselves would switch anyway — they are
    // `:core` JVM resources, not Android resources, so they are not split — which makes the
    // failure worse than an obvious one: Russian tips inside an English settings screen, with no
    // hint that anything is missing.
    //
    // The alternative is Play Feature Delivery and a runtime language download, which is a large
    // dependency and a network permission this app structurally refuses to have. The cost of
    // this instead is 27 strings' worth of APK, which is nothing. `lint`'s AppBundleLocaleChanges
    // check is what flagged it.
    bundle {
        language {
            enableSplit = false
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Full icon set rather than material-icons-core: a handful of specific icons are used
    // (Bedtime, Widgets, Palette...) that aren't guaranteed in the small core set. Release
    // minification strips everything but the icons actually referenced.
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // GlanceTheme itself lives in androidx.glance (transitively via glance-appwidget); the
    // separate glance-material3 artifact only adds a ColorProviders(ColorScheme) bridge this
    // app doesn't use, so it isn't included.
    implementation(libs.androidx.glance.appwidget)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
