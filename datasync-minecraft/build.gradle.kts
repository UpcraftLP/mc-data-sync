import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
}

object Loaders {
    const val FABRIC = "fabric"
    const val NEOFORGE = "neoforge"
}

// TODO neoforge support?
val loader = Loaders.FABRIC

val env: Map<String, String> = System.getenv()

//@formatter:off
val javaVersion =
    if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) 21
    else if (stonecutter.eval(stonecutter.current.version, ">=1.18")) 17
    else if (stonecutter.eval(stonecutter.current.version, ">=1.17")) 16
    else 8
//@formatter:on

class ModData {
    val id = property("mod_id").toString()
    val group = property("maven_group_id").toString()
    val displayName = property("mod_display_name").toString()
    val description = property("mod_description").toString()
    val sourcesUrl = property("sources_url").toString()
    val issuesUrl = property("issues_url").toString()
    val discordUrl = property("discord_url").toString()
    val homepageUrl = property("homepage_url").toString()

    val minecraftVersionRange = property("minecraft_version_range").toString()
}

val mod = ModData()

group = mod.group

val NOW = Date()
val buildTime = env["BUILD_TIME"] ?: SimpleDateFormat("yyyyMMddHHmmss").format(NOW)

val isPreviewBuild = env["TAG"]?.matches(".+-.+".toRegex()) ?: true
val buildNumber = env["TAG"] ?: env["BUILD_NUMBER"]?.let { "build.$it" } ?: buildTime
version = buildString {
    append(env["TAG"] ?: "${stonecutter.current.project}-development")
    if (env["TAG"] == null && isPreviewBuild) append("+$buildNumber")
}
base {
    archivesName.set("datasync-minecraft-${stonecutter.current.project}")
}

sourceSets {
    create("testmod") {
        java {
            compileClasspath += sourceSets["main"].compileClasspath
            runtimeClasspath += sourceSets["main"].runtimeClasspath
        }
    }
}

if (loader == Loaders.FABRIC) {
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
}

repositories {
    if (loader == Loaders.FABRIC) {
        maven("https://maven.terraformersmc.com/releases")
    }
}

dependencies {
    if (loader == Loaders.FABRIC) {
        // To change the versions see the gradle.properties file
        minecraft("com.mojang:minecraft:${stonecutter.current.version}")
        mappings(loom.officialMojangMappings())
        modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version").toString()}")

        // Fabric API. This is technically optional, but you probably want it anyway.
        modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version").toString()}")
        modLocalRuntime("com.terraformersmc:modmenu:${property("modmenu_version").toString()}") {
            isTransitive = false
        }
    }

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
        "minecraft_version" to stonecutter.current.version,
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
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

java {
    withSourcesJar()
    withJavadocJar()

    toolchain {
        if (JavaVersion.current() < JavaVersion.toVersion(javaVersion)) {
            languageVersion = JavaLanguageVersion.of(javaVersion)
            vendor = JvmVendorSpec.MICROSOFT
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
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
        attributes["Implementation-Timestamp"] = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(NOW)
        attributes["Timestamp"] = NOW.toInstant().toEpochMilli()
        attributes["Built-On-Java"] =
            "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})"
        attributes["Built-On-Minecraft"] = stonecutter.current.version
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
        env["MAVEN_UPLOAD_URL"]?.let { uploadUrl ->
            maven {
                url = uri(uploadUrl)
                credentials {
                    username = env["MAVEN_UPLOAD_USERNAME"]
                    password = env["MAVEN_UPLOAD_PASSWORD"]
                }
            }
        }
    }
}
