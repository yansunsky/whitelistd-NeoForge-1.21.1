package com.yansunsky.whitelistd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import com.yansunsky.whitelistd.impl.JsonSearchList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Whitelistd 的 NeoForge 入口类。
 */
@Mod(Whitelistd.MOD_ID)
public final class Whitelistd {
    /** 模组 ID，必须与 neoforge.mods.toml 中的 modId 保持一致。 */
    public static final String MOD_ID = "whitelistd";

    /** 模组日志记录器。 */
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Whitelistd instance;

    private boolean ready;
    private boolean enabled = true;
    private boolean allowAll;
    private boolean showClientWarning = true;
    private final Path configDir;
    private final Path configFile;
    private final WhitelistdConfig config;
    private final SearchList searchList;

    /**
     * 构造函数由 NeoForge 在加载模组时调用。
     *
     * @param modEventBus 模组事件总线，当前阶段无需直接使用
     */
    public Whitelistd(IEventBus modEventBus) {
        if (instance != null) {
            throw new WhitelistdRuntimeException("Whitelistd can only be initialized once");
        }
        instance = this;

        configDir = FMLPaths.CONFIGDIR.get().resolve("Whitelistd");
        configFile = configDir.resolve("config.json");
        config = loadConfig();
        searchList = createSearchList();
        NeoForge.EVENT_BUS.addListener(Commands::register);
        registerClientMisinstallWarning();

        LOGGER.info("Whitelistd NeoForge loaded. Config file: {}", configFile.toAbsolutePath());
    }

    /**
     * 获取 Whitelistd 单例实例。
     *
     * @return Whitelistd 实例
     */
    public static Whitelistd getInstance() {
        if (instance == null) {
            throw new WhitelistdRuntimeException("Whitelistd has not been initialized");
        }
        return instance;
    }

    /**
     * 判断 Whitelistd 是否完成初始化。
     *
     * @return 已完成初始化返回 true
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * 判断 Whitelistd 白名单拦截是否启用。
     *
     * @return 启用返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置 Whitelistd 白名单拦截是否启用。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 判断是否临时允许所有玩家。
     *
     * @return 允许所有玩家返回 true
     */
    public boolean isAllowAll() {
        return allowAll;
    }

    /**
     * 设置是否临时允许所有玩家。
     *
     * @param allowAll 是否允许所有玩家
     */
    public void setAllowAll(boolean allowAll) {
        this.allowAll = allowAll;
    }

    /**
     * 获取配置目录。
     *
     * @return {@code config/Whitelistd} 目录路径
     */
    public Path getConfigDir() {
        return configDir;
    }

    /**
     * 获取配置文件路径。
     *
     * @return {@code config/Whitelistd/config.json} 文件路径
     */
    public Path getConfigFile() {
        return configFile;
    }

    /**
     * 获取当前已加载配置。
     *
     * @return Whitelistd 配置对象
     */
    public WhitelistdConfig getConfig() {
        return config;
    }

    /**
     * 获取当前白名单搜索列表。
     *
     * @return 搜索列表实例
     */
    public SearchList getSearchList() {
        return searchList;
    }

    private WhitelistdConfig loadConfig() {
        try {
            Files.createDirectories(configDir);
            WhitelistdConfig loadedConfig;
            if (Files.exists(configFile)) {
                try (Reader reader = Files.newBufferedReader(configFile)) {
                    loadedConfig = GSON.fromJson(reader, WhitelistdConfig.class);
                }
                if (loadedConfig == null) {
                    loadedConfig = new WhitelistdConfig();
                }
            } else {
                loadedConfig = new WhitelistdConfig();
            }
            writeConfigFile(loadedConfig);
            return loadedConfig;
        } catch (IOException e) {
            throw new WhitelistdRuntimeException("Failed to read/write Whitelistd config file", e);
        }
    }

    private void registerClientMisinstallWarning() {
        if (!config.isDisableClientCheck() && FMLEnvironment.dist == Dist.CLIENT) {
            allowAll = true;
            NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
            LOGGER.warn("Whitelistd is intended for dedicated servers. Client-side allow-all mode has been enabled automatically.");
        }
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!showClientWarning) {
            return;
        }
        MessageHelper.sendSystemMessage(event.getEntity(), Component.translatable("wld.client.only_for_server").withStyle(ChatFormatting.RESET));
        MessageHelper.sendSystemMessage(event.getEntity(), Component.translatable("wld.client.auto_allow_all").withStyle(ChatFormatting.GOLD));
        showClientWarning = false;
    }

    private SearchList createSearchList() {
        SearchList createdSearchList = switch (config.getStorageMode()) {
            case JSON -> new JsonSearchList();
            case HTTP, MYSQL, MONGODB -> throw new WhitelistdRuntimeException(
                    "Storage mode " + config.getStorageMode() + " is not implemented yet"
            );
        };
        createdSearchList.init(config.getSearchMode(), config.isPlayerNameCaseSensitive(), config.getStorageArgs());
        ready = true;
        LOGGER.info("Whitelistd search list initialized with {} mode", config.getStorageMode());
        return createdSearchList;
    }

    private void writeConfigFile(WhitelistdConfig config) throws IOException {
        try (JsonWriter writer = GSON.newJsonWriter(Files.newBufferedWriter(configFile))) {
            writer.setIndent("  ");
            GSON.toJson(config, WhitelistdConfig.class, writer);
        }
    }
}
