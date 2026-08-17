package com.tungsten.fcl.util;

import android.content.Context;

import com.tungsten.fcl.game.KuxiaConfig;
import com.tungsten.fcl.setting.Profile;
import com.tungsten.fcl.setting.Profiles;
import com.tungsten.fcl.ui.TaskDialog;
import com.tungsten.fclcore.download.GameBuilder;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.task.TaskExecutor;
import com.tungsten.fclcore.task.TaskListener;
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog;

import androidx.appcompat.app.AppCompatDialog;

/**
 * 首次启动自动安装游戏核心（MC 1.12.2 + Forge 14.23.5.2847）。
 * 玩家点一次「开始下载」即可，不再需要进下载页手动装版本。
 * 已有版本时不做任何事。
 */
public final class KuxiaAutoInstaller {

    private KuxiaAutoInstaller() {
    }

    public static boolean hasVersion() {
        return !Profiles.getSelectedProfile().getRepository().getVersions().isEmpty();
    }

    public static void installIfNeeded(Context context) {
        if (hasVersion()) {
            return;
        }
        FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(context);
        builder.setCancelable(false);
        builder.setAlertLevel(FCLAlertDialog.AlertLevel.INFO);
        builder.setMessage("检测到尚未安装游戏核心（1.12.2 + Forge），需要联网下载约 300MB（含游戏素材）。\n\n建议连接 WiFi 后开始。");
        builder.setPositiveButton("开始下载", () -> startInstall(context));
        builder.setNegativeButton("暂不下载", () -> {
        });
        builder.create().show();
    }

    private static void startInstall(Context context) {
        GameBuilder builder = Profiles.getSelectedProfile().getDependency().gameBuilder();
        builder.name(KuxiaConfig.GAME_VERSION)
                .gameVersion(KuxiaConfig.GAME_VERSION)
                .version("forge", KuxiaConfig.FORGE_VERSION);

        Task<Void> task = builder.buildAsync()
                .whenComplete(any -> Profiles.getSelectedProfile().getRepository().refreshVersions())
                .thenRunAsync(Schedulers.androidUIThread(), () -> {
                    Profile profile = Profiles.getSelectedProfile();
                    profile.setSelectedVersion(KuxiaConfig.GAME_VERSION);
                });

        TaskDialog pane = new TaskDialog(context, new TaskCancellationAction(AppCompatDialog::dismiss));
        pane.setTitle("安装游戏核心 1.12.2 + Forge " + KuxiaConfig.FORGE_VERSION);
        TaskExecutor executor = task.executor(new TaskListener() {
            @Override
            public void onStop(boolean success, TaskExecutor executor) {
                Schedulers.androidUIThread().execute(() -> {
                    FCLAlertDialog.Builder b = new FCLAlertDialog.Builder(context);
                    b.setCancelable(false);
                    if (success) {
                        b.setAlertLevel(FCLAlertDialog.AlertLevel.INFO);
                        b.setMessage("游戏核心安装完成，点击启动即可进入酷夏探索！");
                    } else {
                        b.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
                        b.setMessage("安装失败：" + (executor.getException() != null ? executor.getException().getLocalizedMessage() : "未知错误") + "\n\n请检查网络后重启 APP 重试。");
                    }
                    b.setPositiveButton(context.getString(com.tungsten.fcl.R.string.dialog_positive), () -> {
                    });
                    b.create().show();
                });
            }
        });
        pane.setExecutor(executor);
        pane.show();
        executor.start();
    }
}
