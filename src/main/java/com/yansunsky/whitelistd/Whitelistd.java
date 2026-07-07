package com.yansunsky.whitelistd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
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

    private final Path configDir;
    private final Path configFile;
    private final WhitelistdConfig config;

    /**
     * 构造函数由 NeoForge 在加载模组时调用。
     */
    public Whitelistd() {
        if (instance != null) {
            throw new WhitelistdRuntimeException("Whitelistd can only be initialized once");
        }
        instance = this;

        configDir = FMLPaths.CONFIGDIR.get().resolve("Whitelistd");
        configFile = configDir.resolve("config.json");
        config = loadConfig();

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

    private void writeConfigFile(WhitelistdConfig config) throws IOException {
        try (JsonWriter writer = GSON.newJsonWriter(Files.newBufferedWriter(configFile))) {
            writer.setIndent("  ");
            GSON.toJson(config, WhitelistdConfig.class, writer);
        }
    }
}
