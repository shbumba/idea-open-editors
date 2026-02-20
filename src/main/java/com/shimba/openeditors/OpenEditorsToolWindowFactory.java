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
        OpenEditorsService service = project.getService(OpenEditorsService.class);

        OpenEditorsListState state = new OpenEditorsListState();
        boolean showFilePath = PropertiesComponent.getInstance(project)
            .getBoolean(ToggleFilePathAction.PROP_KEY, true);
        state.setShowFilePath(showFilePath);

        DefaultListModel<OpenEditorEntry> listModel = new DefaultListModel<>();
        JBList<OpenEditorEntry> fileList = new JBList<>(listModel);
        fileList.setCellRenderer(new OpenEditorCellRenderer(project, state));

        ListModelUpdater updater = new ListModelUpdater(fileList, listModel, service, state);

        DefaultActionGroup gearGroup = new DefaultActionGroup();
        gearGroup.add(new ToggleFilePathAction(project, state, updater::forceRefresh));
        toolWindow.setAdditionalGearActions(gearGroup);

        new OpenEditorsMouseHandler(fileList, listModel, service, state, project, updater::refresh).install();

        fileList.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int size = listModel.getSize();

                for (int i = 0; i < size; i++) {
                    listModel.set(i, listModel.get(i));
                }
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(fileList);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(scrollPane, "", false);
        toolWindow.getContentManager().addContent(content);

        new OpenEditorsListener(project, toolWindow.getDisposable(), updater::refresh);

        updater.refresh();
    }
}
