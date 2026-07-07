package com.yansunsky.whitelistd;

/**
 * 查询白名单搜索列表时使用的搜索模式。
 *
 * <p>该枚举只影响查询操作，不直接决定数据如何存储。</p>
 */
public enum SearchMode {
    /** 仅依据玩家名称查询。 */
    PLAYER_NAME,

    /** 仅依据玩家 UUID 查询。 */
    PLAYER_UUID,

    /** 优先依据 UUID 查询；UUID 不可用时回退到玩家名称。 */
    PLAYER_NAME_OR_UUID
}
