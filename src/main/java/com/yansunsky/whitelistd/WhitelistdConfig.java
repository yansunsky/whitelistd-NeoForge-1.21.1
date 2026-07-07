package com.yansunsky.whitelistd;

import java.util.Arrays;

/**
 * Whitelistd 配置文件对应的数据类。
 *
 * <p>配置会序列化到 {@code config/Whitelistd/config.json}。本类保留 1.20.1 原版配置语义，
 * 不使用 Lombok，以避免注解处理器在 ModDevGradle 环境中的兼容问题。</p>
 */
public class WhitelistdConfig {
    /** 是否禁用客户端检测。 */
    private boolean disableClientCheck = false;

    /** 查询白名单时使用的搜索模式。 */
    private SearchMode searchMode = SearchMode.PLAYER_NAME_OR_UUID;

    /** 验证玩家名称时是否大小写敏感。 */
    private boolean playerNameCaseSensitive = true;

    /** 白名单数据存储方式。 */
    private StorageMode storageMode = StorageMode.JSON;

    /** 存储后端所需参数。JSON 模式下默认为 whitelist.json。 */
    private String[] storageArgs = {"whitelist.json"};

    /** 是否启用 Record 功能。 */
    private boolean enableRecord = true;

    /** 管理白名单指令所需的最低权限等级。 */
    private int permissionLevel = 2;

    /**
     * Gson 反序列化使用的无参构造函数。
     */
    public WhitelistdConfig() {
    }

    public boolean isDisableClientCheck() {
        return disableClientCheck;
    }

    public void setDisableClientCheck(boolean disableClientCheck) {
        this.disableClientCheck = disableClientCheck;
    }

    public SearchMode getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(SearchMode searchMode) {
        this.searchMode = searchMode == null ? SearchMode.PLAYER_NAME_OR_UUID : searchMode;
    }

    public boolean isPlayerNameCaseSensitive() {
        return playerNameCaseSensitive;
    }

    public void setPlayerNameCaseSensitive(boolean playerNameCaseSensitive) {
        this.playerNameCaseSensitive = playerNameCaseSensitive;
    }

    public StorageMode getStorageMode() {
        return storageMode;
    }

    public void setStorageMode(StorageMode storageMode) {
        this.storageMode = storageMode == null ? StorageMode.JSON : storageMode;
    }

    public String[] getStorageArgs() {
        return Arrays.copyOf(storageArgs, storageArgs.length);
    }

    public void setStorageArgs(String[] storageArgs) {
        this.storageArgs = storageArgs == null ? new String[]{"whitelist.json"} : Arrays.copyOf(storageArgs, storageArgs.length);
    }

    public boolean isEnableRecord() {
        return enableRecord;
    }

    public void setEnableRecord(boolean enableRecord) {
        this.enableRecord = enableRecord;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
