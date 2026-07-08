plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.netscope.interceptor"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.okhttp.core)
    implementation(libs.gson)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.netscope"
            artifactId = "interceptor"
            version = "1.0.0"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}