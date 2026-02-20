plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.shimba"
version = "1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
      <ul>
        <li>Persistent open-editors panel dockable on the left side</li>
        <li>Active file is highlighted automatically as you switch tabs</li>
        <li>Pinned tabs appear at the top with a pin icon; click to unpin</li>
        <li>Hover any file to reveal the close (×) button</li>
        <li>Drag &amp; drop to rearrange files within pinned or unpinned groups</li>
        <li>Toggle relative file path display via the gear menu</li>
        <li>Right-click any file for the full editor tab context menu</li>
        <li>Auto-scrolls to keep the active file visible when switching tabs</li>
      </ul>
    """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}
