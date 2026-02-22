package com.shimba.openeditors;

import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.TabInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TabReorderHelper {

    private TabReorderHelper() {
    }

    static void reorderTabs(EditorWindow window, List<VirtualFile> desiredOrder) {
        EditorTabbedContainer tabbedPane = window.getTabbedPane();

        Map<VirtualFile, Integer> orderMap = new HashMap<>();

        for (int i = 0; i < desiredOrder.size(); i++) {
            orderMap.put(desiredOrder.get(i), i);
        }

        tabbedPane.editorTabs.sortTabs((tab1, tab2) -> {
            VirtualFile f1 = extractFile(tab1);
            VirtualFile f2 = extractFile(tab2);
            int p1 = orderMap.getOrDefault(f1, Integer.MAX_VALUE);
            int p2 = orderMap.getOrDefault(f2, Integer.MAX_VALUE);

            return Integer.compare(p1, p2);
        });
    }

    private static VirtualFile extractFile(TabInfo tab) {
        return tab.getObject() instanceof VirtualFile f ? f : null;
    }
}
