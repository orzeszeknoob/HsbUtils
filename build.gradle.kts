import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import net.fabricmc.loom.task.RemapJarTask

plugins {
    idea
    java
    kotlin("jvm") version "1.9.0"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("gg.essential.loom") version "1.3.12"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val modid: String by project
val baseGroup: String by project
val mcVersion: String by project
val version: String by project

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

val devenvMod: Configuration by configurations.creating {
    isTransitive = false
    isVisible = false
}

loom {
    silentMojangMappingsLicense()
        runConfigs {
            getByName("client") {
            programArgs("--mods", devenvMod.resolve().joinToString(",") { it.relativeTo(file("run")).path })
            programArgs("--username", "DevPlayer")
            programArgs("--accessToken", "0")

        }
        forge {
            pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
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
    maven("https://maven.minecraftforge.net/")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://repo.nea.moe/releases")
    maven("https://maven.notenoughupdates.org/releases")
    maven("https://repo.essential.gg/repository/maven-public/")
    maven("https://jitpack.io")
    mavenCentral()
    mavenLocal()
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
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation(libs.libautoupdate)
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation("com.google.code.gson:gson:2.8.9")

    implementation("gg.essential:vigilance:306")
    shadowImplementation("gg.essential:vigilance:306") {
        exclude(group = "gg.essential.elementa")
    }

    implementation("gg.essential:universalcraft-1.8.9-forge") {
        version { strictly("[401,)") }
    }
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


    rename("(.+_at.cfg)", "META-INF/$1")
}


val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    archiveClassifier.set("all-dev")
    configurations = listOf(shadowImplementation, shadowModImpl)

    exclude("META-INF/versions/9/**")
    exclude("META-INF/maven/**")
    exclude("META-INF/services/**")

    relocate("gg.essential.vigilance", "$baseGroup.deps.vigilance") {
        include("gg.essential.vigilance.**")
    }
    relocate("gg.essential.elementa", "$baseGroup.deps.elementa")
    relocate("io.github.moulberry.moulconfig", "$baseGroup.deps.moulconfig")
    relocate("moe.nea.libautoupdate", "$baseGroup.deps.libautoupdate")
    relocate("gg.essential.universalcraft", "$baseGroup.deps.universalcraft")

    mergeServiceFiles()

    exclude("gg/essential/loader/**")
}

tasks.assemble.get().dependsOn(tasks.remapJar)

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}