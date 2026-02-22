package com.shimba.openeditors;

import com.intellij.openapi.fileEditor.impl.EditorWindow;

import java.util.List;

public record EditorGroup(String title, EditorWindow window, List<ListItem.FileEntry> pinned, List<ListItem.FileEntry> unpinned) {}
