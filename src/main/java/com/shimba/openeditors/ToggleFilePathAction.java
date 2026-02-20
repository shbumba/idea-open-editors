package com.shimba.openeditors;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

final class ToggleFilePathAction extends ToggleAction implements DumbAware {

    static final String PROP_KEY = "openEditors.showFilePath";

    private final OpenEditorsListState state;
    private final Runnable refresh;
    private final Project project;

    ToggleFilePathAction(@NotNull Project project, @NotNull OpenEditorsListState state, @NotNull Runnable refresh) {
        super("Show File Path");
        this.state = state;
        this.refresh = refresh;
        this.project = project;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return state.isShowFilePath();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean selected) {
        state.setShowFilePath(selected);
        PropertiesComponent.getInstance(project).setValue(PROP_KEY, selected, true);
        refresh.run();
    }
}
