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

    private static final int POLL_INTERVAL_MS = 2000;
    private static final int DEBOUNCE_MS = 50;
    private static final Set<String> PIN_ACTION_IDS = Set.of("PinActiveTab", "PinActiveTabToggle", "PinActiveEditorTab");

    private final Alarm pollAlarm;
    private final Alarm debounceAlarm;
    private final Runnable onUpdate;

    OpenEditorsListener(Project project, Disposable parentDisposable, Runnable onUpdate) {
        this.onUpdate = onUpdate;
        this.pollAlarm = new Alarm(parentDisposable);
        this.debounceAlarm = new Alarm(parentDisposable);

        subscribeToEditorEvents(project, parentDisposable);
        subscribeToActionEvents(parentDisposable);
        schedulePoll();
    }

    private void subscribeToEditorEvents(Project project, Disposable parentDisposable) {
        project.getMessageBus().connect(parentDisposable).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    scheduleRefresh();
                }

                @Override
                public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    scheduleRefresh();
                }

                @Override
                public void selectionChanged(
                    @NotNull FileEditorManagerEvent event
                ) {
                    scheduleRefresh();
                }
            }
        );
    }

    private void subscribeToActionEvents(Disposable parentDisposable) {
        ApplicationManager.getApplication().getMessageBus().connect(parentDisposable).subscribe(
            AnActionListener.TOPIC, new AnActionListener() {
                @Override
                public void afterActionPerformed(
                    @NotNull AnAction action,
                    @NotNull AnActionEvent event,
                    @NotNull com.intellij.openapi.actionSystem.AnActionResult result
                ) {
                    String id = event.getActionManager().getId(action);

                    if (id != null && PIN_ACTION_IDS.contains(id)) {
                        scheduleRefresh();
                    }
                }
            }
        );
    }

    private void scheduleRefresh() {
        debounceAlarm.cancelAllRequests();
        debounceAlarm.addRequest(() -> ApplicationManager.getApplication().invokeLater(onUpdate), DEBOUNCE_MS);
    }

    private void schedulePoll() {
        pollAlarm.addRequest(
            () -> {
                onUpdate.run();
                schedulePoll();
            }, POLL_INTERVAL_MS
        );
    }
}
