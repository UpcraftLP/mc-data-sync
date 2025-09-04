pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven("https://maven.fabricmc.net")
		maven("https://maven.kikugie.dev/snapshots")
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
	id("dev.kikugie.stonecutter") version "0.7.9"
}

rootProject.name = "DataSync"

stonecutter {
	kotlinController = true
	centralScript = "build.gradle.kts"

	create(rootProject) {
        version("1.19.2-fabric", "1.19.2")
        version("1.20.1-fabric", "1.20.1")
        version("1.21.1-fabric", "1.21.1")
//		version("1.21.1-neoforge")
        version("1.21.9-fabric", "1.21.9")

		vcsVersion = "1.21.1-fabric"
	}
}
