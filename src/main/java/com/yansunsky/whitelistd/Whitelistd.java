package com.yansunsky.whitelistd;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Whitelistd 的 NeoForge 入口类。
 *
 * <p>当前阶段只建立最小可运行骨架：确认 mod id、包结构、资源路径与加载流程正确。
 * 配置、指令、存储与 Mixin 白名单拦截会在后续阶段逐步迁移。</p>
 */
@Mod(Whitelistd.MOD_ID)
public final class Whitelistd {
    /** 模组 ID，必须与 neoforge.mods.toml 中的 modId 保持一致。 */
    public static final String MOD_ID = "whitelistd";

    /** 模组日志记录器。 */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 构造函数由 NeoForge 在加载模组时调用。
     */
    public Whitelistd() {
        LOGGER.info("Whitelistd NeoForge skeleton loaded.");
    }
}
