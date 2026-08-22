package com.tungsten.fcl.game;

/**
 * 酷夏探索定制配置。
 * 服务器专属客户端的常量集中在这里，改服/换端口只动这一处。
 */
public final class KuxiaConfig {

    /** 启动游戏后自动连接的服务器地址（host:port）。 */
    public static final String SERVER_IP = "110.42.32.20:25565";
    /** 游戏与 Forge 版本（与服务端/PC 客户端一致）。 */
    public static final String GAME_VERSION = "1.12.2";
    public static final String FORGE_VERSION = "14.23.5.2847";

    /** 内置整合包在 assets 中的目录名。 */
    public static final String ASSET_DIR = "kuxia_modpack";
    /** 游戏核心在 assets 中的目录与版本戳（核心变化时递增触发重解压）。 */
    public static final String CORE_ASSET_DIR = "kuxia_core";
    public static final int CORE_VERSION = 1;

    /**
     * 整合包版本戳：内置内容更新时递增，
     * 客户端首启/升级后据此重新解压 mods/config/resourcepacks。
     */
    public static final int PACK_VERSION = 3;

    /** DragonCore 大资源分卷在 assets 中的目录名。 */
    public static final String PARTS_DIR = "kuxia_parts";

    /** 按键布局版本戳：assets 布局变更时递增，强制覆盖玩家磁盘旧布局。 */
    public static final int CTRL_VERSION = 5;

    private KuxiaConfig() {
    }
}
