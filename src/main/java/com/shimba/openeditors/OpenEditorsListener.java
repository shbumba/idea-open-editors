package com.shimba.openeditors;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

final class OpenEditorsListener {

    private static final int POLL_INTERVAL_MS = 500;
    private static final Set<String> PIN_ACTION_IDS = Set.of("PinActiveTab", "PinActiveTabToggle", "PinActiveEditorTab");

    private final Alarm alarm;

    OpenEditorsListener(Project project, Disposable parentDisposable, Runnable onUpdate) {
        Runnable invokeUpdate = () -> ApplicationManager.getApplication().invokeLater(onUpdate);

        project.getMessageBus().connect(parentDisposable)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    invokeUpdate.run();
                }

                @Override
                public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    invokeUpdate.run();
                }

                @Override
                public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                    invokeUpdate.run();
                }
            });

        ApplicationManager.getApplication().getMessageBus().connect(parentDisposable)
            .subscribe(AnActionListener.TOPIC, new AnActionListener() {
                @Override
                public void afterActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event,
                    @NotNull com.intellij.openapi.actionSystem.AnActionResult result) {
                    String id = event.getActionManager().getId(action);
                    if (id != null && PIN_ACTION_IDS.contains(id)) {
                        invokeUpdate.run();
                    }
                }
            });

        alarm = new Alarm(parentDisposable);
        schedulePoll(onUpdate);
    }

    private void schedulePoll(Runnable onUpdate) {
        alarm.addRequest(() -> {
            onUpdate.run();
            schedulePoll(onUpdate);
        }, POLL_INTERVAL_MS);
    }
}
