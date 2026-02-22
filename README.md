# Open Editors — Plugin for JetBrains IDEs

Shows currently open editor tabs in a persistent tool window panel, similar to VSCode's **Open Editors** view.

[![JetBrains Plugin](https://img.shields.io/jetbrains/plugin/v/30294-open-editors?label=JetBrains%20Plugin)](https://plugins.jetbrains.com/plugin/30294-open-editors)

## Features

- **Persistent panel** — dockable tool window on the left side, always visible
- **Active file highlight** — the currently focused editor is highlighted automatically
- **Split view & multi-window support** — displays tabs from all editor contexts: main window, split panes, and floating/detached windows
- **Window grouping** — tabs are grouped by their source editor window with section headers (Main Editor, Split View, Window 2, etc.); single-window mode shows a flat list
- **Per-window operations** — click, close, and context menu actions target on the editor window
- **Pinned tabs** — pinned files appear at the top of each group with a pin icon; click the pin to unpin
- **Close button** — hover any file to reveal the close (×) button
- **Drag & drop reorder** — drag items to rearrange tab order within pinned or unpinned groups; constrained to the same editor window
- **Bidirectional tab sync** — reordering in the panel writes back to the IDE tab bar and vice versa
- **File colors** — inherits scope-based background colors and VCS status foreground colors from the IDE
- **File path display** — shows the relative project path below the file name; toggle via the gear menu
- **Context menu** — right-click any file to access the full editor tab context menu
- **Auto-scroll** — scrolls to keep the active file visible when switching tabs

## Screenshots

![screen1.png](src/main/resources/screens/screen1.png)

## Compatibility

Requires IntelliJ Platform 2025.1 or later (build 251+). Works with all JetBrains IDEs based on the IntelliJ Platform.
