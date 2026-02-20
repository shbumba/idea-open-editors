package com.shimba.openeditors;

import com.intellij.openapi.vfs.VirtualFile;

public record OpenEditorEntry(VirtualFile file, boolean pinned, boolean active) {}
