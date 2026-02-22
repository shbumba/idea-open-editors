package com.shimba.openeditors;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class OpenEditorsToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        OpenEditorsDataService dataService = project.getService(OpenEditorsDataService.class);
        OpenEditorsActionService actionService = project.getService(OpenEditorsActionService.class);

        OpenEditorsListState state = new OpenEditorsListState();
        boolean showFilePath = PropertiesComponent.getInstance(project).getBoolean(ToggleFilePathAction.PROP_KEY, true);
        state.setShowFilePath(showFilePath);

        DefaultListModel<ListItem> listModel = new DefaultListModel<>();
        JBList<ListItem> fileList = new JBList<>(listModel) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        fileList.setCellRenderer(new OpenEditorCellRenderer(project, state));
        fileList.setFixedCellHeight(-1);

        ListModelUpdater updater = new ListModelUpdater(fileList, listModel, dataService);

        DefaultActionGroup gearGroup = new DefaultActionGroup();
        gearGroup.add(new ToggleFilePathAction(project, state, updater::forceRefresh));
        toolWindow.setAdditionalGearActions(gearGroup);

        Runnable refresh = updater::refresh;

        new ClickHandler(fileList, listModel, actionService, state, refresh).install();
        new HoverHandler(fileList, listModel, state).install();
        new DragHandler(fileList, listModel, actionService, state, refresh).install();
        new ContextMenuHandler(fileList, listModel, project).install();

        fileList.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // BasicListUI caches cell heights and only re-measures on
                // model change events — a single set triggers full re-layout
                if (listModel.getSize() > 0) {
                    listModel.set(0, listModel.get(0));
                }
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(fileList);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(scrollPane, "", false);
        toolWindow.getContentManager().addContent(content);

        new OpenEditorsListener(project, toolWindow.getDisposable(), refresh);

        updater.refresh();
    }
}
