import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("net.fabricmc.fabric-loom") version "1.15.4"
    id("maven-publish")
}

val javaVersion = 25

class ModData {
    val id = property("mod_id").toString()
    val group = property("maven_group_id").toString()
    val displayName = property("mod_display_name").toString()
    val description = property("mod_description").toString()
    val sourcesUrl = property("sources_url").toString()
    val issuesUrl = property("issues_url").toString()
    val discordUrl = property("discord_url").toString()
    val homepageUrl = property("homepage_url").toString()

    val minecraftVersion = property("minecraft_version").toString()
    val minecraftVersionRange = property("minecraft_version_range").toString()
}

val mod = ModData()

group = mod.group

val now = Date()
val buildTime: String = providers.environmentVariable("BUILD_TIME").orElse(SimpleDateFormat("yyyyMMddHHmmss").format(now)).get()

val isPreviewBuild = providers.environmentVariable("TAG").map { it.matches(".+-.+".toRegex()) }.orElse(true).get()
val buildNumber = providers.environmentVariable("TAG").orElse(providers.environmentVariable("BUILD_NUMBER").map { "build.$it" }).orElse(buildTime).get()
version = buildString {
    providers.environmentVariable("TAG").orNull?.let {
        append(it)
    } ?: run {
        append("${stonecutter.current.project}-development")
        if (isPreviewBuild) {
            append("+$buildNumber")
        }
    }
}
base {
    archivesName.set("datasync-minecraft-${stonecutter.current.project}")
}

stonecutter {
    dependencies["java"] = javaVersion.toString()

    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")
    }
    replacements.string(current.parsed < "26.1") {
        replace("net/minecraft/world/entity/player/Player", "net/minecraft/class_1657")
    }
}

sourceSets.create("testmod") {
    java {
        compileClasspath += sourceSets["main"].compileClasspath
        runtimeClasspath += sourceSets["main"].runtimeClasspath
    }
}

loom {
    runConfigs {
        configureEach {
            runDir("run")
            ideConfigGenerated(true)
            if (project.hasProperty("mc_java_agent_path")) {
                vmArg("-javaagent:${project.findProperty("mc_java_agent_path")}")
            }

            property("fabric.log.level", "info")
            property("java.net.preferIPv4Stack", "true")
        }

        "client" {
            client()
            configName = "Fabric Client"

            if (project.hasProperty("mc_uuid")) {
                programArg("--uuid=${project.findProperty("mc_uuid")}")
            }

            if (project.hasProperty("mc_username")) {
                programArg("--username=${project.findProperty("mc_username")}")
            }

            if (project.hasProperty("mc_java_agent_path")) {
                vmArg("-javaagent:${project.findProperty("mc_java_agent_path")}")
            }
        }

        "server" {
            server()
            configName = "Fabric Server"
        }

        create("testmodClient") {
            client()
            configName = "Fabric Testmod Client"
            source(sourceSets["testmod"])

            if (project.hasProperty("mc_uuid")) {
                programArg("--uuid=${project.findProperty("mc_uuid")}")
            }

            if (project.hasProperty("mc_username")) {
                programArg("--username=${project.findProperty("mc_username")}")
            }
        }

        create("testmodServer") {
            server()
            configName = "Fabric Testmod Server"
            source(sourceSets["testmod"])
        }
    }

    mods {
        create("${mod.id}") {
            sourceSet(sourceSets["main"])
        }

        create("testmod") {
            sourceSet(sourceSets["testmod"])
        }
    }
}

repositories {
    maven("https://maven.terraformersmc.com/releases")
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${mod.minecraftVersion}")
    implementation("net.fabricmc:fabric-loader:${property("fabric_loader_version").toString()}")

    implementation(fabricApi.module("fabric-networking-api-v1", property("fabric_version").toString()))

    findProperty("modmenu_version")?.let {
        localRuntime("com.terraformersmc:modmenu:${it}") {
            exclude(group = "net.fabricmc", module = "fabric-api")
        }
    }

    // make testmod depend on full fabric API
    "testmodImplementation"("net.fabricmc.fabric-api:fabric-api:${property("fabric_version").toString()}")

    "testmodImplementation"(sourceSets["main"].output)
}

tasks.withType<ProcessResources> {
    filteringCharset = "UTF-8"

    val expandProps = mapOf(
        "version" to version,
        "mod_id" to mod.id,
        "mod_display_name" to mod.displayName,
        "mod_description" to mod.description,
        "sources_url" to mod.sourcesUrl,
        "issues_url" to mod.issuesUrl,
        "discord_url" to mod.discordUrl,
        "homepage_url" to mod.homepageUrl,
        "minecraft_version" to mod.minecraftVersion,
        "minecraft_version_range" to mod.minecraftVersionRange,
        "java_version" to javaVersion,

        "fabric_loader_version" to project.property("fabric_loader_version").toString()
    )

    filesMatching("fabric.mod.json") {
        expand(expandProps)
    }
    inputs.properties(expandProps)
}

tasks.withType<JavaCompile> {
    options.release.set(javaVersion)
}

java {
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
        vendor = JvmVendorSpec.MICROSOFT
    }
}

tasks.withType<JavaCompile> {
    options.release.set(javaVersion)
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).let {
        it.tags("reason")
        it.tags("implNote")
    }
}

tasks.jar {
    from("LICENSE.md") {
        rename("LICENSE.md", "LICENSE_${project.base.archivesName.get()}.md")
    }

    manifest {
        attributes["Specification-Title"] = rootProject.name
        attributes["Specification-Version"] = project.version
        attributes["Implementation-Title"] = "${rootProject.name}-${project.name}"
        attributes["Maven-Artifact"] =
            "${mod.group}:datasync-minecraft-${stonecutter.current.project}:${project.version}"
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Timestamp"] = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(now)
        attributes["Timestamp"] = now.toInstant().toEpochMilli()
        attributes["Built-On-Java"] =
            "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})"
        attributes["Built-On-Minecraft"] = mod.minecraftVersion
    }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE.md")) {
        rename("LICENSE.md", "LICENSE_${project.base.archivesName.get()}.md")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "datasync-minecraft-${stonecutter.current.project}"
            from(components["java"])
        }
    }

    repositories {
        providers.environmentVariable("MAVEN_UPLOAD_URL").orNull?.let { url ->
            maven(url) {
                credentials {
                    username = providers.environmentVariable("MAVEN_UPLOAD_USERNAME").orNull
                    password = providers.environmentVariable("MAVEN_UPLOAD_PASSWORD").orNull
                }
            }
        }
    }
}
