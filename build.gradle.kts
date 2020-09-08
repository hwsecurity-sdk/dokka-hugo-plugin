import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
    id("maven-publish")
}

apply {
    plugin("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
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


publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "de.cotech"
            artifactId = "dokka-hugo-plugin"
            version = "1.0"

            from(components["kotlin"])
        }
    }
}
