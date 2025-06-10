import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import net.fabricmc.loom.task.RemapJarTask

plugins {
    idea
    java
    kotlin("jvm") version "2.1.21"
    id("fabric-loom") version "1.10.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val modid: String by project
val baseGroup: String by project
val mcVersion: String by project
val version: String by project

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

val devenvMod: Configuration by configurations.creating {
    isTransitive = false
    isVisible = false
}

loom {
    runConfigs {
        getByName("client") {
            programArgs("--mods", devenvMod.resolve().joinToString(",") { it.relativeTo(file("run")).path })
            programArgs("--username", "DevPlayer")
            programArgs("--accessToken", "0")
        }
    }
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
    java.srcDir(layout.projectDirectory.dir("src/main/kotlin"))
    kotlin.destinationDirectory.set(java.destinationDirectory)
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
            enableLanguageFeature("BreakContinueInInlineLambdas")
        }
    }
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/")
    mavenCentral()
}

val shadowImplementation: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
    isCanBeResolved = true
}

val shadowModImpl: Configuration by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
    isCanBeResolved = true
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.5")
    mappings("net.fabricmc:yarn:1.21.5+build.1")
    modImplementation("net.fabricmc:fabric-loader:0.16.10")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.126.0+1.21.5")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.3+kotlin.2.1.21")
    
    // Cloth Config for configuration
    modApi("me.shedaniel.cloth:cloth-config-fabric:18.0.145") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    
    // Kotlin dependencies
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.compileJava {
    dependsOn(tasks.processResources)
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Jar::class) {
    archiveBaseName.set(modid)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "mcversion" to mcVersion,
            "modid" to modid
        )
    }

    rename("(.+_at.cfg)", "META-INF/$1")
}

tasks.shadowJar {
    archiveClassifier.set("all-dev")
    configurations = listOf(shadowImplementation, shadowModImpl)

    exclude("META-INF/versions/9/**")
    exclude("META-INF/maven/**")
    exclude("META-INF/services/**")

    mergeServiceFiles()
}

tasks.remapJar {
    archiveClassifier.set("")
    inputFile.set(tasks.shadowJar.get().archiveFile)
    archiveFileName.set("$modid-${project.version}.jar")
}

tasks.assemble.get().dependsOn(tasks.remapJar)

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "21"
}