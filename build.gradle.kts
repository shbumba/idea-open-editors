plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.shimba"
version = "1.11"

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
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
      <ul>
        <li><b>Persistent panel</b> — dockable tool window on the left side, always visible</li>
        <li><b>Active file highlight</b> — the currently focused editor is highlighted automatically</li>
        <li><b>Split view & multi-window support</b> — displays tabs from all editor contexts: main window, split panes, and floating/detached windows</li>
        <li><b>Window grouping</b> — tabs are grouped by their source editor window with section headers (Main Editor, Split View, Window 2, etc.); single-window mode shows a flat list</li>
        <li><b>Per-window operations</b> — click, close, and context menu actions target on the editor window</li>
        <li><b>Pinned tabs</b> — pinned files appear at the top of each group with a pin icon; click the pin to unpin</li>
        <li><b>Close button</b> — hover any file to reveal the close (×) button</li>
        <li><b>Drag & drop reorder</b> — drag items to rearrange tab order within pinned or unpinned groups; constrained to the same editor window</li>
        <li><b>Bidirectional tab sync</b> — reordering in the panel writes back to the IDE tab bar and vice versa</li>
        <li><b>File colors</b> — inherits scope-based background colors and VCS status foreground colors from the IDE</li>
        <li><b>File path display</b> — shows the relative project path below the file name; toggle via the gear menu</li>
        <li><b>Context menu</b> — right-click any file to access the full editor tab context menu</li>
        <li><b>Auto-scroll</b> — scrolls to keep the active file visible when switching tabs</li>
      </ul>
    """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    }
}
