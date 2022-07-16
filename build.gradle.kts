plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.5.2"
    id("org.kordamp.gradle.markdown") version "2.2.0"
}

group = "io.github.frykher"
version = System.getenv().getOrDefault("VERSION", "")

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2021.2")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions

    register<Copy>("copyREADME") {
        from("$projectDir/README.md")
        into("$projectDir/build/markdown")
    }

    markdownToHtml {
        dependsOn("copyREADME")
        sourceDir = File("$projectDir/build/markdown")
        outputDir = File("$projectDir/build/html")
    }

    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        dependsOn("markdownToHtml")
        sinceBuild.set("212")
        untilBuild.set("222.*")

        pluginDescription.set(provider {
            file("$projectDir/build/html/README.html").readText()
        })

        changeNotes.set(System.getenv().getOrDefault("CHANGE_NOTES", "None"))



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
