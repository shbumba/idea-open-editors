package com.shimba.openeditors;

import com.intellij.ui.components.JBList;

import javax.swing.*;
import java.util.List;

final class ListModelUpdater {

    private final JBList<ListItem> fileList;
    private final DefaultListModel<ListItem> listModel;
    private final OpenEditorsDataService dataService;

    ListModelUpdater(JBList<ListItem> fileList, DefaultListModel<ListItem> listModel, OpenEditorsDataService dataService) {
        this.fileList = fileList;
        this.listModel = listModel;
        this.dataService = dataService;
    }

    void refresh() {
        List<EditorGroup> groups = dataService.getEditorGroups();
        List<ListItem> items = dataService.buildListItems(groups);

        if (isListUnchanged(items)) {
            return;
        }

        updateModel(items);
        scrollToActive(items);
        fileList.clearSelection();
    }

    void forceRefresh() {
        List<EditorGroup> groups = dataService.getEditorGroups();
        List<ListItem> items = dataService.buildListItems(groups);
        updateModel(items);
        scrollToActive(items);
        fileList.clearSelection();
    }

    private boolean isListUnchanged(List<ListItem> items) {
        if (listModel.getSize() != items.size()) {
            return false;
        }

        for (int i = 0; i < listModel.getSize(); i++) {
            ListItem existing = listModel.getElementAt(i);
            ListItem incoming = items.get(i);

            if (!existing.structurallyEquals(incoming)) {
                return false;
            }
        }

        return true;
    }

    private void updateModel(List<ListItem> items) {
        int oldSize = listModel.getSize();
        int newSize = items.size();
        int common = Math.min(oldSize, newSize);

        for (int i = 0; i < common; i++) {
            listModel.set(i, items.get(i));
        }

        if (newSize > oldSize) {
            for (int i = oldSize; i < newSize; i++) {
                listModel.addElement(items.get(i));
            }
        } else if (oldSize > newSize) {
            for (int i = oldSize - 1; i >= newSize; i--) {
                listModel.remove(i);
            }
        }
    }

    private void scrollToActive(List<ListItem> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof ListItem.FileEntry fe && fe.active()) {
                int idx = i;
                SwingUtilities.invokeLater(() -> fileList.ensureIndexIsVisible(idx));
                return;
            }
        }
    }
}
