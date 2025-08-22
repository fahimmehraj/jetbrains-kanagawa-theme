// See https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api/-project/find-property.html
fun properties(key: String) = project.findProperty(key).toString()

buildscript {
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("com.overzealous:remark:1.1.0")).using(module("com.wavefront:remark:2023-07.07"))
                .because("not available on maven central anymore")
        }
    }
}

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.7.2"
    id("org.kordamp.gradle.markdown") version "2.2.0"
}

group = "io.github.frykher"
// Version is set by CI, GitHub Actions
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

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", properties("platformVersion"))
        bundledPlugin("com.intellij.java")

        pluginVerifier()
        zipSigner()
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
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

        // See https://github.com/catppuccin/jetbrains/blob/27949117de78e8f33f1d5bbeaec975ac9a7c15fe/build.gradle.kts
        // for this rationale
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

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
