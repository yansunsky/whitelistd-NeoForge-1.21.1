package com.yansunsky.whitelistd;

/**
 * 白名单搜索列表的数据存储方式。
 */
public enum StorageMode {
    /** 使用 JSON 文件存储白名单数据。 */
    JSON(1),

    /** 预留 MySQL 存储模式，当前尚未实现。 */
    MYSQL(0),

    /** 预留 MongoDB 存储模式，当前尚未实现。 */
    MONGODB(3),

    /** 预留 HTTP API 存储模式，当前尚未实现。 */
    HTTP(3);

    private final int argNumber;

    StorageMode(int argNumber) {
        this.argNumber = argNumber;
    }

    /**
     * 获取该存储模式需要的参数数量。
     *
     * @return 参数数量
     */
    public int getArgNumber() {
        return argNumber;
    }
}
