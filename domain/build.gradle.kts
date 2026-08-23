// Pure Kotlin/JVM module: the entire business-rules layer (habit types,
// streak math, XP/leveling, consistency scoring, quest & achievement rules,
// statistics aggregation). It has ZERO Android dependency on purpose so it
// compiles and unit-tests with a plain JVM, independent of the Android
// Gradle Plugin / Android SDK, and is trivially reusable if a second
// front-end (e.g. Wear OS, desktop) is ever added.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
