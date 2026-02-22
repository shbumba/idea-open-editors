package com.shimba.openeditors;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

final class DragHandler {

    private static final int DRAG_THRESHOLD = 5;

    private final JBList<ListItem> fileList;
    private final DefaultListModel<ListItem> listModel;
    private final OpenEditorsActionService actionService;
    private final OpenEditorsListState state;
    private final Runnable refresh;

    private int dragFrom = -1;
    private Point dragStart;

    DragHandler(JBList<ListItem> fileList, DefaultListModel<ListItem> listModel, OpenEditorsActionService actionService,
        OpenEditorsListState state, Runnable refresh) {
        this.fileList = fileList;
        this.listModel = listModel;

        this.actionService = actionService;
        this.state = state;
        this.refresh = refresh;
    }

    void install() {
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased();
            }
        });

        fileList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
        });
    }

    private void handleMousePressed(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }

        ListHitTest.HitResult hit = ListHitTest.hitTest(fileList, e);

        if (hit == null) {
            return;
        }

        if (ListHitTest.isOverActionButton(e, hit.bounds())) {
            return;
        }

        ListItem item = listModel.getElementAt(hit.index());

        if (item instanceof ListItem.GroupHeader) {
            return;
        }

        dragFrom = hit.index();
        dragStart = e.getPoint();
        fileList.clearSelection();
    }

    private void handleMouseReleased() {
        if (dragFrom >= 0 && state.getDropTarget() >= 0) {
            commitDrop();
        }

        state.setSuppressNextClick(state.isDragging());
        dragFrom = -1;
        dragStart = null;
        state.setDropTarget(-1);
        state.setDragging(false);

        if (state.isSuppressNextClick()) {
            fileList.clearSelection();
        }

        fileList.repaint();
    }

    private void handleMouseDragged(MouseEvent e) {
        if (dragFrom < 0 || dragStart == null) {
            return;
        }

        if (dragStart.distance(e.getPoint()) < DRAG_THRESHOLD) {
            return;
        }

        state.setDragging(true);

        int index = fileList.locationToIndex(e.getPoint());
        int newTarget = computeDropTarget(e, index);

        if (newTarget >= 0) {
            newTarget = clampToGroupBounds(newTarget);
        }

        if (newTarget != state.getDropTarget()) {
            state.setDropTarget(newTarget);
            fileList.repaint();
        }
    }

    private int computeDropTarget(MouseEvent e, int index) {
        if (index < 0) {
            return -1;
        }

        Rectangle bounds = fileList.getCellBounds(index, index);

        if (bounds == null) {
            return -1;
        }

        int midY = bounds.y + bounds.height / 2;

        return e.getY() < midY ? index : index + 1;
    }

    private int clampToGroupBounds(int target) {
        GroupBounds bounds = findGroupBounds(dragFrom);

        if (bounds == null) {
            return target;
        }

        boolean fromPinned = dragFrom < bounds.pinnedEnd;

        if (fromPinned) {
            return Math.max(bounds.start, Math.min(target, bounds.pinnedEnd));
        }

        return Math.max(bounds.pinnedEnd, Math.min(target, bounds.end));
    }

    private void commitDrop() {
        int fromIdx = dragFrom;
        int toIdx = state.getDropTarget();

        if (fromIdx == toIdx || fromIdx + 1 == toIdx) {
            return;
        }

        ListItem draggedItem = listModel.getElementAt(fromIdx);

        if (!(draggedItem instanceof ListItem.FileEntry draggedEntry)) {
            return;
        }

        GroupBounds bounds = findGroupBounds(fromIdx);

        if (bounds == null) {
            return;
        }

        List<VirtualFile> files = collectGroupFiles(bounds, fromIdx, toIdx);

        actionService.reorderTabsInWindow(draggedEntry.window(), files);
        actionService.openFileInWindow(draggedEntry.file(), draggedEntry.window());
        refresh.run();
    }

    private List<VirtualFile> collectGroupFiles(GroupBounds bounds, int fromIdx, int toIdx) {
        List<VirtualFile> display = new ArrayList<>();

        for (int i = bounds.start; i < bounds.end; i++) {
            ListItem item = listModel.getElementAt(i);

            if (item instanceof ListItem.FileEntry fe) {
                display.add(fe.file());
            }
        }

        int relFrom = fromIdx - bounds.start;
        int relTo = toIdx - bounds.start;

        VirtualFile dragged = display.remove(relFrom);
        int insertAt = relTo > relFrom ? relTo - 1 : relTo;

        display.add(insertAt, dragged);

        return display;
    }

    private GroupBounds findGroupBounds(int dragIndex) {
        if (dragIndex < 0 || dragIndex >= listModel.getSize()) {
            return null;
        }

        ListItem draggedItem = listModel.getElementAt(dragIndex);

        if (!(draggedItem instanceof ListItem.FileEntry)) {
            return null;
        }

        int start = findGroupStart(dragIndex);
        int end = findGroupEnd(dragIndex);
        int pinnedEnd = findPinnedEnd(start, end);

        return new GroupBounds(start, end, pinnedEnd);
    }

    private int findGroupStart(int fromIndex) {
        for (int i = fromIndex - 1; i >= 0; i--) {
            if (listModel.getElementAt(i) instanceof ListItem.GroupHeader) {
                return i + 1;
            }
        }

        return 0;
    }

    private int findGroupEnd(int fromIndex) {
        for (int i = fromIndex + 1; i < listModel.getSize(); i++) {
            if (listModel.getElementAt(i) instanceof ListItem.GroupHeader) {
                return i;
            }
        }

        return listModel.getSize();
    }

    private int findPinnedEnd(int start, int end) {
        for (int i = start; i < end; i++) {
            ListItem item = listModel.getElementAt(i);

            if (item instanceof ListItem.FileEntry fe && !fe.pinned()) {
                return i;
            }
        }

        return end;
    }

    private record GroupBounds(int start, int end, int pinnedEnd) {}
}
