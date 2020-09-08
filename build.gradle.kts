import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
}

tasks.withType(KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit"))
    
    compileOnly("org.jetbrains.dokka:dokka-core:1.4.0")
    implementation("org.jetbrains.dokka:dokka-base:1.4.0")
    implementation("org.jetbrains.dokka:gfm-plugin:1.4.0")
}
