import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.net.URI

plugins {
    id("java")
    id("com.rikonardo.papermake") version "1.0.6"
    id("maven-publish")
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.dokka") version "1.9.20"
    kotlin("plugin.serialization") version "2.0.20"
}

group = "io.github.eingruenesbeb"
version = "0.7.2"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = URI("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = URI("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "bstats-bukkit"
        url = URI("https://repo.codemc.io/repository/maven-public/")
    }
    maven {
        name = "bstats-bungeecord"
        url = URI("https://repo.codemc.io/repository/maven-public/")
    }
    maven {
        name = "spicord"
        url = URI("https://repo.spicord.org")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("net.dv8tion:JDA:5.1.0")
    compileOnly("org.spicord:spicord-common:5.4.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.6.3")


    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.127.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.6.3")
    testImplementation("io.mockk:mockk:1.13.12")
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType(JavaCompile::class.java).configureEach {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}


tasks.test {
    useJUnitPlatform()

    maxHeapSize = "4G"

    testLogging {
        events("passed")
    }
}


tasks.dokkaHtml {
    outputDirectory.set(File(projectDir.path + "/docs"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}
