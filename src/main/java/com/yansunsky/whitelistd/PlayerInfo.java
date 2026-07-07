package com.yansunsky.whitelistd;

import java.util.Objects;
import java.util.UUID;

/**
 * 玩家信息，由名称和 UUID 两部分组成。
 *
 * <p>玩家名称一定存在，UUID 可以为 {@code null}，表示仅按玩家名存储或查询。</p>
 */
public final class PlayerInfo {
    private final String name;
    private final UUID uuid;

    /**
     * 创建无 UUID 的玩家信息。
     *
     * @param name 玩家名称
     */
    public PlayerInfo(String name) {
        this(name, null);
    }

    /**
     * 创建玩家信息。
     *
     * @param name 玩家名称
     * @param uuid 玩家 UUID，可以为 {@code null}
     */
    public PlayerInfo(String name, UUID uuid) {
        this.name = Objects.requireNonNull(name, "name");
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }
}
