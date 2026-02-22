package com.shimba.openeditors;

import com.intellij.ui.components.JBList;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class ClickHandler {

    private final JBList<ListItem> fileList;
    private final DefaultListModel<ListItem> listModel;
    private final OpenEditorsActionService actionService;
    private final OpenEditorsListState state;
    private final Runnable refresh;

    ClickHandler(
        JBList<ListItem> fileList,
        DefaultListModel<ListItem> listModel,
        OpenEditorsActionService actionService,
        OpenEditorsListState state,
        Runnable refresh
    ) {
        this.fileList = fileList;
        this.listModel = listModel;
        this.actionService = actionService;
        this.state = state;
        this.refresh = refresh;
    }

    void install() {
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e);
            }
        });
    }

    private void handleClick(MouseEvent e) {
        if (state.isSuppressNextClick()) {
            state.setSuppressNextClick(false);

            return;
        }

        if (!SwingUtilities.isLeftMouseButton(e)) {
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

        if (ListHitTest.isOverActionButton(e, hit.bounds())) {
            handleActionButton(entry);
        } else {
            actionService.openFileInWindow(entry.file(), entry.window());
        }
    }

    private void handleActionButton(ListItem.FileEntry entry) {
        if (entry.pinned()) {
            actionService.unpinFileInWindow(entry.file(), entry.window());
            actionService.openFileInWindow(entry.file(), entry.window());
            refresh.run();
        } else {
            actionService.closeFileInWindow(entry.file(), entry.window());
        }
    }
}
