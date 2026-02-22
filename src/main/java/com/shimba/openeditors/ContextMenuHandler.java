package com.shimba.openeditors;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class ContextMenuHandler {

    private final JBList<ListItem> fileList;
    private final DefaultListModel<ListItem> listModel;
    private final Project project;

    ContextMenuHandler(JBList<ListItem> fileList, DefaultListModel<ListItem> listModel, Project project) {
        this.fileList = fileList;
        this.listModel = listModel;
        this.project = project;
    }

    void install() {
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopup(e);
            }
        });
    }

    private void handlePopup(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        ListHitTest.HitResult hit = ListHitTest.hitTest(fileList, e);

        if (hit == null) {
            return;
        }

        ListItem item = listModel.getElementAt(hit.index());

        if (!(item instanceof ListItem.FileEntry entry)) {
            return;
        }

        fileList.setSelectedIndex(hit.index());
        showEditorTabPopup(entry, e);
    }

    private void showEditorTabPopup(ListItem.FileEntry entry, MouseEvent e) {
        AnAction action = ActionManager.getInstance().getAction("EditorTabPopupMenu");

        if (!(action instanceof ActionGroup group)) {
            return;
        }

        // IntelliJ tab actions read the window's selected tab, not
        // VIRTUAL_FILE from DataContext — select the right-clicked file first
        entry.window().setSelectedComposite(entry.file(), false);

        DataContext ctx = buildDataContext(entry);
        ActionPopupMenu popup = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.EDITOR_TAB_POPUP, group);
        popup.setDataContext(() -> ctx);
        popup.getComponent().show(fileList, e.getX(), e.getY());
    }

    private DataContext buildDataContext(ListItem.FileEntry entry) {
        return SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.VIRTUAL_FILE, entry.file())
            .add(EditorWindow.DATA_KEY, entry.window())
            .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, fileList)
            .build();
    }
}
