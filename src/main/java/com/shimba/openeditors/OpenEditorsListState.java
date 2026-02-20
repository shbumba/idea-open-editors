package com.shimba.openeditors;

final class OpenEditorsListState {

    private int pinnedCount;
    private int hoveredCellIndex = -1;
    private boolean actionButtonHovered;
    private int dropTarget = -1;
    private boolean dragging;
    private boolean showFilePath = true;

    int getPinnedCount() {
        return pinnedCount;
    }

    void setPinnedCount(int pinnedCount) {
        this.pinnedCount = pinnedCount;
    }

    int getHoveredCellIndex() {
        return hoveredCellIndex;
    }

    void setHoveredCellIndex(int hoveredCellIndex) {
        this.hoveredCellIndex = hoveredCellIndex;
    }

    boolean isActionButtonHovered() {
        return actionButtonHovered;
    }

    void setActionButtonHovered(boolean actionButtonHovered) {
        this.actionButtonHovered = actionButtonHovered;
    }

    int getDropTarget() {
        return dropTarget;
    }

    void setDropTarget(int dropTarget) {
        this.dropTarget = dropTarget;
    }

    boolean isDragging() {
        return dragging;
    }

    void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    boolean isShowFilePath() {
        return showFilePath;
    }

    void setShowFilePath(boolean showFilePath) {
        this.showFilePath = showFilePath;
    }
}
