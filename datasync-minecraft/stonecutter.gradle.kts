plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21.1-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val loaders = listOf("fabric", "neoforge")
    val current = stonecutter.current.project.substringAfterLast('-')

    consts(current, loaders)
}

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "project"
    ofTask("build")
}

stonecutter registerChiseled tasks.register("chiseledAssemble", stonecutter.chiseled) {
    group = "project"
    ofTask("assemble")
}

stonecutter registerChiseled tasks.register("chiseledPublish", stonecutter.chiseled) {
    group = "project"
    ofTask("publish")
}

stonecutter registerChiseled tasks.register("chiseledPublishToMavenLocal", stonecutter.chiseled) {
    group = "project"
    ofTask("publishToMavenLocal")
}
