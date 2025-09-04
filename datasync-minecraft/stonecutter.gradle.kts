plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21.1-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val loaders = listOf("fabric", "neoforge")
    val current = node.metadata.project.substringAfterLast('-')

    constants.match(current, loaders)
}
