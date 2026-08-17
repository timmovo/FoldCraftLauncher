package com.tungsten.fcl.util;

import android.content.Context;

import com.tungsten.fcl.game.KuxiaConfig;
import com.tungsten.fclauncher.utils.FCLPath;
import com.tungsten.fclcore.util.Logging;
import com.tungsten.fclcore.util.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * 内置整合包安装器：首启（或整合包版本升级后）把 APK assets 里的
 * kuxia_modpack 解压到公共游戏目录，玩家无需手动拷贝任何文件。
 *
 * 语义：
 * - mods / config / resourcepacks 由服务器控制，版本升级时整体重建；
 * - options.txt / servers.dat 仅在缺失时写入，不覆盖玩家已有设置。
 */
public final class KuxiaPackInstaller {

    private static final String STAMP_FILE = ".kuxia_pack";

    /** 服务器控制、随版本重建的目录。 */
    private static final String[] MANAGED_DIRS = {"mods", "config", "resourcepacks"};

    /** 仅缺失时写入的文件。 */
    private static final String[] ONCE_FILES = {"options.txt", "servers.dat"};

    private KuxiaPackInstaller() {
    }

    /** 已是最新内置整合包版本则快速返回。 */
    public static boolean isUpToDate() {
        File stamp = new File(FCLPath.SHARED_COMMON_DIR, STAMP_FILE);
        try {
            return stamp.isFile() && Integer.parseInt(new String(Files.readAllBytes(stamp.toPath()), StandardCharsets.UTF_8).trim()) >= KuxiaConfig.PACK_VERSION;
        } catch (Exception e) {
            return false;
        }
    }

    public static void installIfNeeded(Context context) {
        if (isUpToDate()) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            File gameDir = new File(FCLPath.SHARED_COMMON_DIR);
            if (!gameDir.isDirectory() && !gameDir.mkdirs()) {
                Logging.LOG.log(Level.WARNING, "KuxiaPack: cannot create game dir " + gameDir);
                return;
            }
            for (String dir : MANAGED_DIRS) {
                File target = new File(gameDir, dir);
                if (target.isDirectory()) {
                    FileUtils.cleanDirectoryQuietly(target);
                }
                RuntimeUtils.copyAssets(context, KuxiaConfig.ASSET_DIR + "/" + dir, target.getAbsolutePath());
            }
            joinSplitAssets(context);
            for (String name : ONCE_FILES) {
                File target = new File(gameDir, name);
                if (!target.isFile()) {
                    RuntimeUtils.copyAssets(context, KuxiaConfig.ASSET_DIR + "/" + name, target.getAbsolutePath());
                }
            }
            File stamp = new File(gameDir, STAMP_FILE);
            FileUtils.writeText(stamp, String.valueOf(KuxiaConfig.PACK_VERSION));
            Logging.LOG.log(Level.INFO, "KuxiaPack: installed v" + KuxiaConfig.PACK_VERSION
                    + " in " + (System.currentTimeMillis() - start) + " ms");
        } catch (IOException e) {
            Logging.LOG.log(Level.SEVERE, "KuxiaPack: install failed", e);
        }
    }

    /**
     * DragonCore 的 Resource.zip 超过 GitHub 单文件 100MB 限制，
     * 以 95MB 分卷打包进 assets（kuxia_parts/），此处按序拼接还原到
     * resourcepacks/DragonCore/Resource.zip。加密 zip 原样搬运，不影响龙核加载。
     */
    private static void joinSplitAssets(Context context) {
        String[] parts;
        try {
            parts = context.getAssets().list(KuxiaConfig.PARTS_DIR);
        } catch (IOException e) {
            return;
        }
        if (parts == null || parts.length == 0) {
            return;
        }
        Arrays.sort(parts);
        File out = new File(FCLPath.SHARED_COMMON_DIR, "resourcepacks/DragonCore/Resource.zip");
        out.getParentFile().mkdirs();
        try (OutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[65536];
            for (String part : parts) {
                try (InputStream is = context.getAssets().open(KuxiaConfig.PARTS_DIR + "/" + part)) {
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        os.write(buf, 0, n);
                    }
                }
            }
        } catch (IOException e) {
            Logging.LOG.log(Level.SEVERE, "KuxiaPack: join split assets failed", e);
        }
    }
}
