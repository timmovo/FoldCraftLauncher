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
 * 内置整合包安装器：首启（或版本升级后）把 APK assets 里的内容
 * 解压到公共游戏目录，玩家无需联网下载、无需手动拷贝任何文件。
 *
 * - 核心部分（kuxia_core：versions/libraries/assets = MC 1.12.2 + Forge 2847）
 *   独立版本戳，正常情况只解压一次；
 * - 整合包部分（kuxia_modpack）：mods/config/resourcepacks 随版本重建；
 *   options.txt/servers.dat 仅缺失时写入，不覆盖玩家已有设置。
 * - 安装是否"最新"不只看戳记，还校验关键文件实存（自愈）：
 *   戳记可能来自旧版本残留而文件已被清理/损坏。
 */
public final class KuxiaPackInstaller {

    private static final String STAMP_FILE = ".kuxia_pack";
    private static final String CORE_STAMP_FILE = ".kuxia_core";

    /** 服务器控制、随版本重建的目录。 */
    private static final String[] MANAGED_DIRS = {"mods", "config", "resourcepacks"};

    /** 仅缺失时写入的文件。 */
    private static final String[] ONCE_FILES = {"options.txt", "servers.dat"};

    /** 游戏核心目录。 */
    private static final String[] CORE_DIRS = {"versions", "libraries", "assets"};

    /** 自愈校验：mods jar 数量（与 assets/kuxia_modpack 内一致）。 */
    private static final int EXPECTED_MODS = 16;
    /** 布局版本戳文件（存 control 目录）。 */
    private static final String CTRL_STAMP_FILE = ".kuxia_ctrl";
    /** 布局文件名（与 assets/controllers 一致）。 */
    private static final String CTRL_FILE = "00000000.json";

    private KuxiaPackInstaller() {
    }

    public static void installIfNeeded(Context context) {
        installCoreIfNeeded(context);
        installModpackIfNeeded(context);
        installControllerIfNeeded(context);
    }

    /**
     * 布局更新：FCL 只在磁盘无布局时播种 assets 默认布局，
     * APK 升级后新布局永远不会到达老玩家。此处按 CTRL_VERSION
     * 强制覆盖 /FCL/control/00000000.json（玩家自定义会被重置，
     * 服务器专属客户端可接受；版本戳不变时不触碰）。
     */
    private static void installControllerIfNeeded(Context context) {
        File ctrlDir = new File(FCLPath.CONTROLLER_DIR);
        File stamp = new File(ctrlDir, CTRL_STAMP_FILE);
        if (readStamp(stamp) >= KuxiaConfig.CTRL_VERSION) {
            return;
        }
        try {
            if (!ctrlDir.isDirectory() && !ctrlDir.mkdirs()) {
                return;
            }
            RuntimeUtils.copyAssets(context, "controllers/" + CTRL_FILE, new File(ctrlDir, CTRL_FILE).getAbsolutePath());
            FileUtils.writeText(stamp, String.valueOf(KuxiaConfig.CTRL_VERSION));
            Logging.LOG.log(Level.INFO, "KuxiaPack: controller v" + KuxiaConfig.CTRL_VERSION + " installed");
        } catch (IOException e) {
            Logging.LOG.log(Level.SEVERE, "KuxiaPack: controller install failed", e);
        }
    }

    /** 核心解压：独立版本戳，CORE_VERSION 升级时重做。 */
    private static void installCoreIfNeeded(Context context) {
        File stamp = new File(FCLPath.SHARED_COMMON_DIR, CORE_STAMP_FILE);
        if (readStamp(stamp) >= KuxiaConfig.CORE_VERSION) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            File gameDir = new File(FCLPath.SHARED_COMMON_DIR);
            if (!gameDir.isDirectory() && !gameDir.mkdirs()) {
                Logging.LOG.log(Level.WARNING, "KuxiaPack: cannot create game dir " + gameDir);
                return;
            }
            for (String dir : CORE_DIRS) {
                RuntimeUtils.copyAssets(context, KuxiaConfig.CORE_ASSET_DIR + "/" + dir, new File(gameDir, dir).getAbsolutePath());
            }
            FileUtils.writeText(stamp, String.valueOf(KuxiaConfig.CORE_VERSION));
            Logging.LOG.log(Level.INFO, "KuxiaPack: core v" + KuxiaConfig.CORE_VERSION
                    + " installed in " + (System.currentTimeMillis() - start) + " ms");
        } catch (IOException e) {
            Logging.LOG.log(Level.SEVERE, "KuxiaPack: core install failed", e);
        }
    }
    /** 已是最新内置整合包版本则快速返回。 */
    public static boolean isUpToDate() {
        return readStamp(new File(FCLPath.SHARED_COMMON_DIR, STAMP_FILE)) >= KuxiaConfig.PACK_VERSION
                && filesIntact();
    }

    /**
     * 文件实存校验：戳记可能来自旧版本残留（升级安装/目录被清理过），
     * 关键内容缺失时必须重装，而不是只信戳记。
     */
    private static boolean filesIntact() {
        File gameDir = new File(FCLPath.SHARED_COMMON_DIR);
        File[] mods = new File(gameDir, "mods").listFiles((d, n) -> n.endsWith(".jar"));
        if (mods == null || mods.length < EXPECTED_MODS) {
            return false;
        }
        File kxts = new File(gameDir, "resourcepacks/kxts.zip");
        if (!kxts.isFile() || kxts.length() < 8 * 1024 * 1024) {
            return false;
        }
        File dc = new File(gameDir, "resourcepacks/DragonCore/Resource.zip");
        return dc.isFile() && dc.length() > 160L * 1024 * 1024;
    }

    private static int readStamp(File stamp) {
        try {
            return stamp.isFile() ? Integer.parseInt(new String(Files.readAllBytes(stamp.toPath()), StandardCharsets.UTF_8).trim()) : -1;
        } catch (Exception e) {
            return -1;
        }
    }


    private static void installModpackIfNeeded(Context context) {
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
            Logging.LOG.log(Level.INFO, "KuxiaPack: modpack v" + KuxiaConfig.PACK_VERSION
                    + " installed in " + (System.currentTimeMillis() - start) + " ms");
        } catch (IOException e) {
            Logging.LOG.log(Level.SEVERE, "KuxiaPack: modpack install failed", e);
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
