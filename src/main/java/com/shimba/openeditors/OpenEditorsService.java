package com.shimba.openeditors;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.tabs.TabInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service(Service.Level.PROJECT)
public final class OpenEditorsService {

    private final Project project;

    public OpenEditorsService(Project project) {
        this.project = project;
    }

    public List<OpenEditorEntry> getOpenEditors() {
        FileEditorManagerEx manager = getManagerEx();
        List<EditorWindow> mainWindows = getMainWindows();

        Set<VirtualFile> activeFiles = new LinkedHashSet<>();
        for (EditorWindow w : mainWindows) {
            VirtualFile selected = w.getSelectedFile();
            if (selected != null) activeFiles.add(selected);
        }

        LinkedHashSet<VirtualFile> seen = new LinkedHashSet<>();

        EditorWindow current = manager.getCurrentWindow();
        if (current != null && mainWindows.contains(current)) {
            collectFiles(current, seen);
        }

        for (EditorWindow window : mainWindows) {
            collectFiles(window, seen);
        }

        List<OpenEditorEntry> pinned = new ArrayList<>();
        List<OpenEditorEntry> unpinned = new ArrayList<>();

        for (VirtualFile file : seen) {
            boolean isPinned = isFilePinned(file, mainWindows);
            boolean isActive = activeFiles.contains(file);
            OpenEditorEntry entry = new OpenEditorEntry(file, isPinned, isActive);

            if (isPinned) {
                pinned.add(entry);
            } else {
                unpinned.add(entry);
            }
        }

        List<OpenEditorEntry> result = new ArrayList<>(pinned.size() + unpinned.size());
        result.addAll(pinned);
        result.addAll(unpinned);

        return result;
    }

    public void closeFile(VirtualFile file) {
        FileEditorManager.getInstance(project).closeFile(file);
    }

    public void openFile(VirtualFile file) {
        FileEditorManager.getInstance(project).openFile(file, true);
    }

    public void reorderTabs(List<VirtualFile> desiredOrder) {
        FileEditorManagerEx manager = getManagerEx();
        List<EditorWindow> mainWindows = getMainWindows();
        EditorWindow window = manager.getCurrentWindow();

        if (window == null || !mainWindows.contains(window)) {
            if (mainWindows.isEmpty()) {
                return;
            }

            window = mainWindows.get(0);
        }

        EditorTabbedContainer tabbedPane = window.getTabbedPane();

        if (tabbedPane == null) {
            return;
        }

        Map<VirtualFile, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < desiredOrder.size(); i++) {
            orderMap.put(desiredOrder.get(i), i);
        }

        tabbedPane.editorTabs.sortTabs((tab1, tab2) -> {
            VirtualFile f1 = getFileFromTab(tab1);
            VirtualFile f2 = getFileFromTab(tab2);
            int p1 = orderMap.getOrDefault(f1, Integer.MAX_VALUE);
            int p2 = orderMap.getOrDefault(f2, Integer.MAX_VALUE);

            return Integer.compare(p1, p2);
        });
    }

    public void unpinFile(VirtualFile file) {
        for (EditorWindow window : getMainWindows()) {
            safeSetPinned(window, file, false);
        }
    }

    private FileEditorManagerEx getManagerEx() {
        return (FileEditorManagerEx) FileEditorManager.getInstance(project);
    }

    private List<EditorWindow> getMainWindows() {
        FileEditorManagerEx manager = getManagerEx();
        var mainFrame = WindowManager.getInstance().getFrame(project);
        if (mainFrame == null) {
            return List.of();
        }
        var mainSplitters = manager.getSplittersFor(mainFrame);
        return Arrays.stream(manager.getWindows())
            .filter(w -> w.getOwner() == mainSplitters)
            .toList();
    }

    private boolean safeIsFilePinned(EditorWindow window, VirtualFile file) {
        try {
            return window.isFilePinned(file);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private void safeSetPinned(EditorWindow window, VirtualFile file, boolean pinned) {
        try {
            window.setFilePinned(file, pinned);
        } catch (IllegalArgumentException ignored) {
            // File not in this window
        }
    }

    private void collectFiles(EditorWindow window, LinkedHashSet<VirtualFile> seen) {
        seen.addAll(window.getFileList());
    }

    private VirtualFile getFileFromTab(TabInfo tab) {
        Object obj = tab.getObject();
        if (obj instanceof VirtualFile file) {
            return file;
        }
        return null;
    }

    private boolean isFilePinned(VirtualFile file, List<EditorWindow> windows) {
        return windows.stream().anyMatch(w -> safeIsFilePinned(w, file));
    }
}
