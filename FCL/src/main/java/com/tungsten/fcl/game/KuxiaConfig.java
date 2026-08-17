package com.tungsten.fcl.game;

/**
 * 酷夏探索定制配置。
 * 服务器专属客户端的常量集中在这里，改服/换端口只动这一处。
 */
public final class KuxiaConfig {

    /** 启动游戏后自动连接的服务器地址（host:port）。 */
    public static final String SERVER_IP = "110.42.32.20:25565";

    /** 内置整合包在 assets 中的目录名。 */
    public static final String ASSET_DIR = "kuxia_modpack";

    /**
     * 整合包版本戳：内置内容更新时递增，
     * 客户端首启/升级后据此重新解压 mods/config/resourcepacks。
     */
    public static final int PACK_VERSION = 1;

    /** DragonCore 大资源分卷在 assets 中的目录名。 */
    public static final String PARTS_DIR = "kuxia_parts";

    private KuxiaConfig() {
    }
}
