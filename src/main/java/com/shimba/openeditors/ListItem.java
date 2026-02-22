package com.shimba.openeditors;

import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;

public sealed interface ListItem {

    boolean structurallyEquals(ListItem other);

    record GroupHeader(String title, EditorWindow window) implements ListItem {

        @Override
        public boolean structurallyEquals(ListItem other) {
            return other instanceof GroupHeader g && title.equals(g.title) && window == g.window;
        }
    }

    record FileEntry(VirtualFile file, boolean pinned, boolean active, EditorWindow window) implements ListItem {

        @Override
        public boolean structurallyEquals(ListItem other) {
            return other instanceof FileEntry e && file.equals(e.file) && pinned == e.pinned && active == e.active && window == e.window;
        }
    }
}
