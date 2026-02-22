package com.shimba.openeditors;

import com.intellij.ui.components.JBList;

import java.awt.*;
import java.awt.event.MouseEvent;

final class ListHitTest {

    record HitResult(int index, Rectangle bounds) {
    }

    private ListHitTest() {
    }

    static HitResult hitTest(JBList<ListItem> list, MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());

        if (index < 0) {
            return null;
        }

        Rectangle bounds = list.getCellBounds(index, index);

        if (bounds == null || !bounds.contains(e.getPoint())) {
            return null;
        }

        return new HitResult(index, bounds);
    }

    static boolean isOverActionButton(MouseEvent e, Rectangle bounds) {
        int actionX = bounds.x + bounds.width - OpenEditorCellRenderer.ACTION_BUTTON_WIDTH;

        return e.getX() >= actionX;
    }
}
