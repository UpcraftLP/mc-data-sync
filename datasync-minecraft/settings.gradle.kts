pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven("https://maven.fabricmc.net")
		maven("https://maven.kikugie.dev/snapshots")
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
	id("dev.kikugie.stonecutter") version "0.6-alpha.11"
}

rootProject.name = "DataSync"

stonecutter {
	kotlinController = true
	centralScript = "build.gradle.kts"

	create(rootProject) {
		vers("1.19.2-fabric", "1.19.2")
		vers("1.20.1-fabric", "1.20.1")
		vers("1.21.1-fabric", "1.21.1")
//		vers("1.21.1-neoforge", "1.21.1")
		vcsVersion = "1.21.1-fabric"
	}
}
