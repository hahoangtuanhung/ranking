// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

buildscript {

    extra["minSdkVersion"] = 24
    extra["compileSdkVersion"] = 34
    extra["targetSdkVersion"] = 34

    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath(libs.publisher.v060)
    }
}