package com.shimba.openeditors;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.FileColorManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

final class OpenEditorCellRenderer extends JPanel implements ListCellRenderer<OpenEditorEntry> {

    static final int ACTION_BUTTON_WIDTH = 22;
    static final int BORDER_SIZE = 4;
    private static final int EXTRA_TEXT_PADDING = 8;

    private final JLabel fileIcon = new JLabel();
    private final JLabel fileName = new JLabel();
    private final JLabel filePath = new JLabel();
    private final ActionIconPanel actionPanel;
    private final JPanel westPanel;
    private final JPanel textPanel;
    private final VirtualFile projectBase;
    private final OpenEditorsListState state;
    private final Project project;
    private JList<?> ownerList;
    private boolean filePathVisible = true;
    private boolean dndTop;
    private boolean dndBottom;

    OpenEditorCellRenderer(Project project, OpenEditorsListState state) {
        super(new BorderLayout());
        setOpaque(false);

        String basePath = project.getBasePath();
        this.projectBase = basePath != null ? LocalFileSystem.getInstance().findFileByPath(basePath) : null;
        this.state = state;
        this.project = project;

        westPanel = new JPanel(new GridBagLayout());
        westPanel.setOpaque(false);
        GridBagConstraints iconGbc = new GridBagConstraints();
        iconGbc.insets = new Insets(0, 4, 0, 8);
        westPanel.add(fileIcon, iconGbc);

        textPanel = new JPanel(new BorderLayout(0, 1));
        textPanel.setOpaque(false);
        filePath.setForeground(JBColor.GRAY);
        filePath.setFont(JBUI.Fonts.smallFont());
        textPanel.add(fileName, BorderLayout.NORTH);
        textPanel.add(filePath, BorderLayout.SOUTH);

        actionPanel = new ActionIconPanel();

        add(westPanel, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = ownerList != null ? ownerList.getBackground() : UIUtil.getListBackground();

            int width = getWidth();
            int height = getHeight();

            g2.setColor(base);
            g2.fillRect(0, 0, width, height);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, width, height, 6, 6);

            if (dndTop || dndBottom) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setColor(JBColor.namedColor("DragAndDrop.borderColor", JBColor.BLUE));

                if (dndTop) {
                    g2.fillRect(2, 0, width - 4, 2);
                } else {
                    g2.fillRect(2, height - 2, width - 4, 2);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (ownerList != null && ownerList.getWidth() > 0) {
            d.width = Math.min(d.width, ownerList.getWidth());
        }
        return d;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends OpenEditorEntry> list, OpenEditorEntry entry, int index,
        boolean isSelected, boolean cellHasFocus) {

        this.ownerList = list;
        VirtualFile file = entry.file();

        fileIcon.setIcon(file.getFileType().getIcon());

        Font baseFont = list.getFont();
        fileName.setFont(baseFont.deriveFont(Font.PLAIN));

        String nameText = file.getPresentableName();
        String relPath = projectBase != null ? VfsUtilCore.getRelativePath(file, projectBase, '/') : null;
        String pathText = relPath != null ? relPath : file.getPresentableUrl();

        if (list.getWidth() > 0) {
            Insets insets = getInsets();
            int westW = westPanel.getPreferredSize().width;
            int eastW = actionPanel.getPreferredSize().width;
            int availW = list.getWidth() - westW - eastW - insets.left - insets.right - EXTRA_TEXT_PADDING;

            if (availW > 0) {
                FontMetrics nameFm = fileName.getFontMetrics(fileName.getFont());
                FontMetrics pathFm = filePath.getFontMetrics(filePath.getFont());
                nameText = truncateFromEnd(nameText, nameFm, availW);
                pathText = truncateFromStart(pathText, pathFm, availW);
            }
        }

        fileName.setText(nameText);

        boolean showPath = state.isShowFilePath();
        if (showPath != filePathVisible) {
            textPanel.remove(fileName);
            textPanel.add(fileName, showPath ? BorderLayout.NORTH : BorderLayout.CENTER);
            filePathVisible = showPath;
        }
        if (showPath) {
            filePath.setText(pathText);
            filePath.setVisible(true);
        } else {
            filePath.setText("");
            filePath.setVisible(false);
        }

        int dropTarget = state.getDropTarget();
        int listSize = list.getModel().getSize();

        dndTop = dropTarget >= 0 && index == dropTarget;
        dndBottom = dropTarget >= 0 && dropTarget == listSize && index == listSize - 1;

        setBorder(JBUI.Borders.empty(BORDER_SIZE));

        boolean isHovered = dropTarget < 0 && index == state.getHoveredCellIndex();

        applyColors(entry, cellHasFocus, isHovered);

        if (entry.pinned()) {
            actionPanel.setIcon(AllIcons.Actions.PinTab);
        } else {
            actionPanel.setIcon(AllIcons.Actions.Close);
        }

        boolean isHoveredAction = isHovered && state.isActionButtonHovered();
        actionPanel.setHovered(isHoveredAction);

        setToolTipText(relPath != null ? relPath : file.getPresentableUrl());

        return this;
    }

    private void applyColors(OpenEditorEntry entry, boolean cellHasFocus, boolean isHovered) {
        if (entry.active()) {
            Color bg = UIUtil.getListSelectionBackground(cellHasFocus);
            Color fg = UIUtil.getListSelectionForeground(cellHasFocus);
            setBackground(bg);
            fileName.setForeground(fg);
            filePath.setForeground(fg);
        } else {
            Color scopeBg = FileColorManager.getInstance(project).getFileColor(entry.file());
            Color rowBg = scopeBg != null ? scopeBg
                    : (isHovered ? UIUtil.getListBackground() : UIUtil.getTreeBackground());
            setBackground(rowBg);

            FileStatus status = FileStatusManager.getInstance(project).getStatus(entry.file());
            Color vcsColor = status != null ? status.getColor() : null;
            fileName.setForeground(vcsColor != null ? vcsColor : UIUtil.getListForeground());
            filePath.setForeground(JBColor.GRAY);
        }
    }

    private static String truncateFromEnd(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "\u2026";
        int ellipsisW = fm.stringWidth(ellipsis);

        for (int i = text.length() - 1; i >= 0; i--) {
            String sub = text.substring(0, i);
            if (fm.stringWidth(sub) + ellipsisW <= maxWidth) {
                return sub + ellipsis;
            }
        }

        return ellipsis;
    }

    private static String truncateFromStart(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "\u2026";
        int ellipsisW = fm.stringWidth(ellipsis);

        for (int i = 1; i < text.length(); i++) {
            String sub = text.substring(i);
            if (fm.stringWidth(sub) + ellipsisW <= maxWidth) {
                return ellipsis + sub;
            }
        }

        return ellipsis;
    }

    private static class ActionIconPanel extends JPanel {

        private final JLabel iconLabel = new JLabel();
        private boolean hovered;

        ActionIconPanel() {
            super(new GridBagLayout());
            setOpaque(false);
            add(iconLabel);
        }

        void setIcon(Icon icon) {
            iconLabel.setIcon(icon);
        }

        void setHovered(boolean hovered) {
            this.hovered = hovered;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(ACTION_BUTTON_WIDTH, ACTION_BUTTON_WIDTH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (hovered) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color hoverBg = UIManager.getColor("ActionButton.hoverBackground");

                if (hoverBg == null) {
                    hoverBg = new JBColor(new Color(0, 0, 0, 25), new Color(255, 255, 255, 25));
                }

                g2.setColor(hoverBg);
                int w = getWidth();
                int y = (getHeight() - ACTION_BUTTON_WIDTH) / 2;
                g2.fillRoundRect(0, y, w, w, w, w);
                g2.dispose();
            }

            super.paintComponent(g);
        }
    }
}
