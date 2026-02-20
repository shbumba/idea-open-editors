package com.shimba.openeditors;

import com.intellij.ui.components.JBList;

import javax.swing.*;
import java.util.List;

final class ListModelUpdater {

    private final JBList<OpenEditorEntry> fileList;
    private final DefaultListModel<OpenEditorEntry> listModel;
    private final OpenEditorsService service;
    private final OpenEditorsListState state;

    ListModelUpdater(JBList<OpenEditorEntry> fileList, DefaultListModel<OpenEditorEntry> listModel, OpenEditorsService service,
        OpenEditorsListState state) {
        this.fileList = fileList;
        this.listModel = listModel;
        this.service = service;
        this.state = state;
    }

    void refresh() {
        List<OpenEditorEntry> entries = service.getOpenEditors();

        if (isListUnchanged(entries)) {
            return;
        }

        state.setPinnedCount((int) entries.stream().filter(OpenEditorEntry::pinned).count());
        updateModel(entries);
        scrollToActive(entries);
        fileList.clearSelection();
    }

    void forceRefresh() {
        List<OpenEditorEntry> entries = service.getOpenEditors();
        state.setPinnedCount((int) entries.stream().filter(OpenEditorEntry::pinned).count());
        updateModel(entries);
        scrollToActive(entries);
        fileList.clearSelection();
    }

    private boolean isListUnchanged(List<OpenEditorEntry> entries) {
        if (listModel.getSize() != entries.size()) {
            return false;
        }

        for (int i = 0; i < listModel.getSize(); i++) {
            OpenEditorEntry existing = listModel.getElementAt(i);
            OpenEditorEntry incoming = entries.get(i);

            if (!existing.file().equals(incoming.file()) || existing.pinned() != incoming.pinned()
                || existing.active() != incoming.active()) {
                return false;
            }
        }
        return true;
    }

    private void updateModel(List<OpenEditorEntry> entries) {
        int oldSize = listModel.getSize();
        int newSize = entries.size();
        int common = Math.min(oldSize, newSize);

        for (int i = 0; i < common; i++) {
            listModel.set(i, entries.get(i));
        }

        if (newSize > oldSize) {
            for (int i = oldSize; i < newSize; i++) {
                listModel.addElement(entries.get(i));
            }
        } else if (oldSize > newSize) {
            for (int i = oldSize - 1; i >= newSize; i--) {
                listModel.remove(i);
            }
        }
    }

    private void scrollToActive(List<OpenEditorEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).active()) {
                int idx = i;
                SwingUtilities.invokeLater(() -> fileList.ensureIndexIsVisible(idx));
                return;
            }
        }
    }
}
