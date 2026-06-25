plugins {
    id("dev.kikugie.stonecutter")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.4.1"
}
stonecutter active "1.21.1-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val loaders = listOf("fabric", "neoforge")
    val current = node.metadata.project.substringAfterLast('-')

    constants.match(current, loaders)
}

stonecutter handlers {
    inherit("java", "json")
}
