package com.shimba.openeditors;

import com.intellij.ui.components.JBList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

final class HoverHandler {

    private final JBList<ListItem> fileList;
    private final DefaultListModel<ListItem> listModel;
    private final OpenEditorsListState state;

    HoverHandler(JBList<ListItem> fileList, DefaultListModel<ListItem> listModel, OpenEditorsListState state) {
        this.fileList = fileList;
        this.listModel = listModel;
        this.state = state;
    }

    void install() {
        fileList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMoved(e);
            }
        });

        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                handleMouseExited();
            }
        });
    }

    private void handleMouseMoved(MouseEvent e) {
        int index = fileList.locationToIndex(e.getPoint());
        boolean overAction = false;

        if (index >= 0) {
            Rectangle bounds = fileList.getCellBounds(index, index);

            if (bounds != null && bounds.contains(e.getPoint())) {
                ListItem item = listModel.getElementAt(index);
                if (item instanceof ListItem.GroupHeader) {
                    index = -1;
                } else {
                    overAction = ListHitTest.isOverActionButton(e, bounds);
                }
            } else {
                index = -1;
            }
        }

        if (index != state.getHoveredCellIndex() || overAction != state.isActionButtonHovered()) {
            state.setHoveredCellIndex(index);
            state.setActionButtonHovered(overAction);
            fileList.repaint();
        }
    }

    private void handleMouseExited() {
        if (state.getHoveredCellIndex() != -1 || state.isActionButtonHovered()) {
            state.setHoveredCellIndex(-1);
            state.setActionButtonHovered(false);
            fileList.repaint();
        }
    }
}
