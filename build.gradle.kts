import java.net.URI

plugins {
    id("java")
    id("com.rikonardo.papermake") version "1.0.5"
    id("maven-publish")
}

group = "io.github.eingruenesbeb"
version = "0.5.0da"

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
        name = "jitpack"
        url = URI("https://jitpack.io")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.github.DevLeoko:AdvancedBan:v2.3.0")
    compileOnly("net.dv8tion:JDA:5.0.0-beta.5")
    compileOnly("com.github.Spicord.Spicord:spicord-common:v5-SNAPSHOT")
    // Manually test functionality on PaperMake-dev-server for now.
    /*
    testCompileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito:mockito-core:5.1.1")
     */
}

val targetJavaVersion = 17
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

/*
tasks.test {
    useJUnitPlatform()

    maxHeapSize = "1G"

    testLogging {
        events("passed")
    }
}
 */

tasks.javadoc {
    setDestinationDir(File(projectDir.path + "/docs"))
    (options as StandardJavadocDocletOptions)
        .tags
        ?.addAll(listOf("apiNote:a:API Note:", "implSpec:a:Implementation Requirements:", "implNote:a:Implementation Note:"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
