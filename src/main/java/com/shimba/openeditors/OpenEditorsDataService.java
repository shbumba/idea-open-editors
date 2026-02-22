package com.shimba.openeditors;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorsSplitters;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class OpenEditorsDataService {

    private final Project project;

    public OpenEditorsDataService(Project project) {
        this.project = project;
    }

    public List<EditorGroup> getEditorGroups() {
        FileEditorManagerEx manager = getManagerEx();
        EditorWindow[] allWindows = manager.getWindows();

        if (allWindows.length == 0) {
            return List.of();
        }

        EditorWindow currentWindow = manager.getCurrentWindow();
        EditorsSplitters mainSplitters = findMainSplitters(manager);
        ClassifiedWindows classified = classifyWindows(allWindows, mainSplitters);

        List<EditorGroup> groups = new ArrayList<>();
        int totalWindows = classified.main.size() + classified.floating.size();
        boolean needsTitles = totalWindows > 1;

        buildMainFrameGroups(classified.main, currentWindow, needsTitles, groups);
        buildFloatingGroups(classified.floating, currentWindow, needsTitles, groups);

        return groups;
    }

    public List<ListItem> buildListItems(List<EditorGroup> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }

        boolean showHeaders = groups.size() > 1;
        List<ListItem> items = new ArrayList<>();

        for (EditorGroup group : groups) {
            if (showHeaders) {
                items.add(new ListItem.GroupHeader(group.title(), group.window()));
            }

            items.addAll(group.pinned());
            items.addAll(group.unpinned());
        }

        return items;
    }

    private void buildMainFrameGroups(
        List<EditorWindow> mainWindows,
        EditorWindow currentWindow,
        boolean needsTitles,
        List<EditorGroup> groups
    ) {
        for (int i = 0; i < mainWindows.size(); i++) {
            EditorWindow window = mainWindows.get(i);
            String title = needsTitles ? mainFrameTitle(i) : "";
            groups.add(buildGroup(title, window, currentWindow));
        }
    }

    private void buildFloatingGroups(
        List<EditorWindow> floatingWindows,
        EditorWindow currentWindow,
        boolean needsTitles,
        List<EditorGroup> groups
    ) {
        for (int i = 0; i < floatingWindows.size(); i++) {
            EditorWindow window = floatingWindows.get(i);
            String title = needsTitles ? "Window " + (i + 1) : "";
            groups.add(buildGroup(title, window, currentWindow));
        }
    }

    private EditorGroup buildGroup(String title, EditorWindow window, EditorWindow currentWindow) {
        boolean isFocusedWindow = window == currentWindow;
        VirtualFile selectedFile = window.getSelectedFile();
        List<ListItem.FileEntry> pinned = new ArrayList<>();
        List<ListItem.FileEntry> unpinned = new ArrayList<>();

        for (VirtualFile file : window.getFileList()) {
            boolean isPinned = safeIsFilePinned(window, file);
            boolean isActive = isFocusedWindow && file.equals(selectedFile);
            ListItem.FileEntry entry = new ListItem.FileEntry(file, isPinned, isActive, window);

            if (isPinned) {
                pinned.add(entry);
            } else {
                unpinned.add(entry);
            }
        }

        return new EditorGroup(title, window, pinned, unpinned);
    }

    private static String mainFrameTitle(int windowIndex) {
        if (windowIndex == 0) {
            return "Main Editor";
        }
        if (windowIndex == 1) {
            return "Split View";
        }
        return "Split View " + windowIndex;
    }

    private record ClassifiedWindows(List<EditorWindow> main, List<EditorWindow> floating) {
    }

    private static ClassifiedWindows classifyWindows(EditorWindow[] allWindows, EditorsSplitters mainSplitters) {
        List<EditorWindow> main = new ArrayList<>();
        List<EditorWindow> floating = new ArrayList<>();

        for (EditorWindow window : allWindows) {
            if (mainSplitters != null && window.getOwner() == mainSplitters) {
                main.add(window);
            } else {
                floating.add(window);
            }
        }

        return new ClassifiedWindows(main, floating);
    }

    private EditorsSplitters findMainSplitters(FileEditorManagerEx manager) {
        JFrame mainFrame = WindowManager.getInstance().getFrame(project);

        if (mainFrame == null) {
            return null;
        }

        return manager.getSplittersFor(mainFrame);
    }

    private static boolean safeIsFilePinned(EditorWindow window, VirtualFile file) {
        try {
            return window.isFilePinned(file);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private FileEditorManagerEx getManagerEx() {
        return (FileEditorManagerEx) FileEditorManager.getInstance(project);
    }
}
