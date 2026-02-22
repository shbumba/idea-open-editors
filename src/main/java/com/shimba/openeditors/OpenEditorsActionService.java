package com.shimba.openeditors;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorsSplitters;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;

import javax.swing.*;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class OpenEditorsActionService {

    private final Project project;

    public OpenEditorsActionService(Project project) {
        this.project = project;
    }

    public void openFileInWindow(VirtualFile file, EditorWindow window) {
        FileEditorManagerEx manager = getManagerEx();

        if (isMainFrameWindow(manager, window)) {
            manager.setCurrentWindow(window);
            new OpenFileDescriptor(project, file).navigate(true);
        } else {
            // setCurrentWindow throws for windows outside the main
            // splitters — select the tab directly and bring the frame
            // to front
            window.setSelectedComposite(file, true);
            java.awt.Window frame = SwingUtilities.getWindowAncestor(
                window.getOwner()
            );
            if (frame != null) {
                frame.toFront();
            }
        }
    }

    private boolean isMainFrameWindow(
        FileEditorManagerEx manager,
        EditorWindow window
    ) {
        JFrame mainFrame = WindowManager.getInstance().getFrame(project);
        if (mainFrame == null) {
            return false;
        }
        EditorsSplitters mainSplitters = manager.getSplittersFor(mainFrame);
        return window.getOwner() == mainSplitters;
    }

    public void closeFileInWindow(VirtualFile file, EditorWindow window) {
        FileEditorManagerEx manager = getManagerEx();
        manager.closeFile(file, window);
    }

    public void unpinFileInWindow(VirtualFile file, EditorWindow window) {
        safeSetPinned(window, file, false);
    }

    public void reorderTabsInWindow(EditorWindow window, List<VirtualFile> order) {
        TabReorderHelper.reorderTabs(window, order);
    }

    private static void safeSetPinned(EditorWindow window, VirtualFile file, boolean pinned) {
        try {
            window.setFilePinned(file, pinned);
        } catch (IllegalArgumentException ignored) {
            // IntelliJ throws IAE when the file is not in this window
        }
    }

    private FileEditorManagerEx getManagerEx() {
        return (FileEditorManagerEx) FileEditorManager.getInstance(project);
    }
}
