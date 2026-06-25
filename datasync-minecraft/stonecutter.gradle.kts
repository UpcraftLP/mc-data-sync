plugins {
    id("dev.kikugie.stonecutter")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.4.1"
}
stonecutter active "1.21.1-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val loaders = listOf("fabric", "neoforge")
    val cr = node.metadata.project.substringAfterLast('-')

    constants.match(cr, loaders)

    replacements {
        replacements.string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        replacements.string(current.parsed >= "26.1") {
            replace("classTweaker v2 named", "classTweaker v2 official")
        }
    }
}

stonecutter handlers {
    inherit("java", "json", "classtweaker", "ct", "toml")
}
