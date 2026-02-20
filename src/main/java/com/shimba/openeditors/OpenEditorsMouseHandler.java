package com.shimba.openeditors;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

final class OpenEditorsMouseHandler {

    private final JBList<OpenEditorEntry> fileList;
    private final DefaultListModel<OpenEditorEntry> listModel;
    private final OpenEditorsService service;
    private final OpenEditorsListState state;
    private final Project project;
    private final Runnable refresh;
    // Set synchronously in mouseReleased when a drag just ended; cleared in mouseClicked.
    // Prevents the stale isDragging() race where invokeLater reset arrived after mouseClicked.
    private boolean suppressNextClick = false;

    OpenEditorsMouseHandler(JBList<OpenEditorEntry> fileList, DefaultListModel<OpenEditorEntry> listModel, OpenEditorsService service,
        OpenEditorsListState state, Project project, Runnable refresh) {
        this.fileList = fileList;
        this.listModel = listModel;
        this.service = service;
        this.state = state;
        this.project = project;
        this.refresh = refresh;
    }

    void install() {
        addClickListener();
        addHoverTracking();
        addDragSupport();
    }

    private record HitResult(int index, Rectangle bounds) {}

    private HitResult hitTest(MouseEvent e) {
        int index = fileList.locationToIndex(e.getPoint());

        if (index < 0) {
            return null;
        }

        Rectangle bounds = fileList.getCellBounds(index, index);

        if (bounds == null || !bounds.contains(e.getPoint())) {
            return null;
        }

        return new HitResult(index, bounds);
    }

    private boolean isOverActionButton(MouseEvent e, Rectangle bounds) {
        int actionX = bounds.x + bounds.width - OpenEditorCellRenderer.ACTION_BUTTON_WIDTH;

        return e.getX() >= actionX;
    }

    private void addClickListener() {
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // suppressNextClick is set synchronously when a drag just ended, avoiding
                // the stale isDragging() race (invokeLater reset arrives after mouseClicked).
                if (suppressNextClick) {
                    suppressNextClick = false;
                    return;
                }

                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                HitResult hit = hitTest(e);

                if (hit == null) {
                    return;
                }

                OpenEditorEntry entry = listModel.getElementAt(hit.index);

                if (isOverActionButton(e, hit.bounds)) {
                    if (entry.pinned()) {
                        service.unpinFile(entry.file());
                        service.openFile(entry.file());
                        refresh.run();
                    } else {
                        service.closeFile(entry.file());
                    }
                } else {
                    service.openFile(entry.file());
                }
            }

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

    private void addHoverTracking() {
        fileList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = fileList.locationToIndex(e.getPoint());
                boolean overAction = false;

                if (index >= 0) {
                    Rectangle bounds = fileList.getCellBounds(index, index);

                    if (bounds != null && bounds.contains(e.getPoint())) {
                        overAction = isOverActionButton(e, bounds);
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
        });

        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (state.getHoveredCellIndex() != -1 || state.isActionButtonHovered()) {
                    state.setHoveredCellIndex(-1);
                    state.setActionButtonHovered(false);
                    fileList.repaint();
                }
            }
        });
    }

    private void addDragSupport() {
        int[] dragFrom = {-1};
        Point[] dragStart = {null};

        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                HitResult hit = hitTest(e);

                if (hit == null) {
                    return;
                }

                if (isOverActionButton(e, hit.bounds)) {
                    return;
                }

                dragFrom[0] = hit.index;
                dragStart[0] = e.getPoint();
                fileList.clearSelection();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragFrom[0] >= 0 && state.getDropTarget() >= 0) {
                    int fromIdx = dragFrom[0];
                    int toIdx = state.getDropTarget();

                    if (fromIdx != toIdx && fromIdx + 1 != toIdx) {
                        List<VirtualFile> display = new ArrayList<>();
                        int size = listModel.getSize();

                        for (int i = 0; i < size; i++) {
                            display.add(listModel.getElementAt(i).file());
                        }

                        VirtualFile dragged = display.remove(fromIdx);
                        int insertAt = toIdx > fromIdx ? toIdx - 1 : toIdx;
                        display.add(insertAt, dragged);
                        service.reorderTabs(display);
                        refresh.run();
                    }
                }

                // Capture drag state before resetting: if a real drag just ended,
                // suppress the corresponding mouseClicked so it doesn't open a file.
                // Resetting synchronously (not via invokeLater) ensures mouseClicked
                // sees the correct state in the same EDT cycle.
                suppressNextClick = state.isDragging();
                dragFrom[0] = -1;
                dragStart[0] = null;
                state.setDropTarget(-1);
                state.setDragging(false);
                if (suppressNextClick) {
                    // After a real drag, ensure no lingering selection remains.
                    fileList.clearSelection();
                }
                fileList.repaint();
            }
        });

        fileList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragFrom[0] < 0 || dragStart[0] == null) {
                    return;
                }

                if (dragStart[0].distance(e.getPoint()) < 5) {
                    return;
                }

                state.setDragging(true);

                int index = fileList.locationToIndex(e.getPoint());
                int newTarget = -1;

                if (index >= 0) {
                    Rectangle bounds = fileList.getCellBounds(index, index);
                    if (bounds != null) {
                        int midY = bounds.y + bounds.height / 2;
                        newTarget = e.getY() < midY ? index : index + 1;
                    }
                }

                if (newTarget >= 0) {
                    int pc = state.getPinnedCount();
                    boolean fromPinned = dragFrom[0] < pc;
                    if (fromPinned) {
                        newTarget = Math.max(0, Math.min(newTarget, pc));
                    } else {
                        newTarget = Math.max(pc, Math.min(newTarget, listModel.getSize()));
                    }
                }

                if (newTarget != state.getDropTarget()) {
                    state.setDropTarget(newTarget);
                    fileList.repaint();
                }
            }
        });
    }

    private void handlePopup(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        HitResult hit = hitTest(e);

        if (hit == null) {
            return;
        }

        fileList.setSelectedIndex(hit.index);
        OpenEditorEntry entry = listModel.getElementAt(hit.index);
        showEditorTabPopup(entry, e);
    }

    private void showEditorTabPopup(OpenEditorEntry entry, MouseEvent e) {
        AnAction action = ActionManager.getInstance().getAction("EditorTabPopupMenu");

        if (!(action instanceof ActionGroup group)) {
            return;
        }

        DataContext ctx = buildDataContext(entry);
        ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.EDITOR_TAB_POPUP, group);
        popupMenu.setDataContext(() -> ctx);
        popupMenu.getComponent().show(fileList, e.getX(), e.getY());
    }

    private DataContext buildDataContext(OpenEditorEntry entry) {
        FileEditorManagerEx manager = (FileEditorManagerEx) FileEditorManager.getInstance(project);
        return SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).add(CommonDataKeys.VIRTUAL_FILE, entry.file())
            .add(EditorWindow.DATA_KEY, manager.getCurrentWindow()).add(PlatformCoreDataKeys.CONTEXT_COMPONENT, fileList).build();
    }
}
