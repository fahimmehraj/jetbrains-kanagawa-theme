plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.13.3"
    id("org.kordamp.gradle.markdown") version "2.2.0"
}

group = "io.github.frykher"
version = System.getenv().getOrDefault("VERSION", "")
val markdownPath = File("$projectDir/build/markdown")
if (!markdownPath.exists()) {
    markdownPath.mkdirs()
}
val htmlPath = File("$projectDir/build/html")
if (!htmlPath.exists()) {
    htmlPath.mkdirs()
}

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2023.1")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions

    register<Copy>("copyREADME") {
        from("$projectDir/README.md")
        into(markdownPath)
    }

    register("createChangeNotes") {
        doLast {
            val mdChangeNotes = System.getenv().getOrDefault("CHANGE_NOTES", "None")
            file("$markdownPath/CHANGE_NOTES.md").writeText(mdChangeNotes)
        }
    }

    markdownToHtml {
        dependsOn("copyREADME")
        dependsOn("createChangeNotes")
        sourceDir = markdownPath
        outputDir = htmlPath
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        dependsOn("markdownToHtml")
        sinceBuild.set("212")
        untilBuild.set("233.*")

        pluginDescription.set(provider {
            file("$htmlPath/README.html").readText()
        })

        changeNotes.set(provider {
            file("$htmlPath/CHANGE_NOTES.html").readText()
        })

    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
