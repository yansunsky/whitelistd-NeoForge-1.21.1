package com.yansunsky.whitelistd;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 白名单搜索列表接口。
 */
public interface SearchList {
    /**
     * 初始化搜索列表。
     *
     * @param mode 搜索模式
     * @param playerNameCaseSensitive 玩家名称大小写是否敏感
     * @param args 存储后端参数
     */
    void init(SearchMode mode, boolean playerNameCaseSensitive, String[] args);

    /**
     * 向搜索列表中添加项目。
     *
     * @param player 玩家信息
     * @return 添加操作结果
     */
    AddItemState addItem(PlayerInfo player);

    /**
     * 从搜索列表中移除项目。
     *
     * @param player 玩家信息
     * @return 移除操作结果
     */
    RemoveItemState removeItem(PlayerInfo player);

    /**
     * 查询玩家是否存在于搜索列表。
     *
     * @param player 玩家信息
     * @return 查询结果
     */
    QueryResult query(PlayerInfo player);

    /**
     * 清空搜索列表。
     *
     * @return 清空操作结果
     */
    ClearState clear();

    /**
     * 获取列表项目总数。
     *
     * @return 项目总数
     */
    int size();

    /**
     * 获取所有项目。
     *
     * @return 所有项目
     */
    Iterable<PlayerInfo> getItems();

    /**
     * 获取所有项目并按条件筛选。
     *
     * @param filter 筛选器
     * @return 筛选后的项目
     */
    default Iterable<PlayerInfo> getItems(Predicate<PlayerInfo> filter) {
        Objects.requireNonNull(filter, "filter");
        return () -> new Iterator<>() {
            private final Iterator<PlayerInfo> allItems = getItems().iterator();
            private PlayerInfo next;
            private boolean nextReady;

            @Override
            public boolean hasNext() {
                if (nextReady) {
                    return true;
                }
                while (allItems.hasNext()) {
                    PlayerInfo candidate = allItems.next();
                    if (filter.test(candidate)) {
                        next = candidate;
                        nextReady = true;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public PlayerInfo next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                PlayerInfo result = next;
                next = null;
                nextReady = false;
                return result;
            }
        };
    }

    /**
     * 以索引位置获取多个项目。
     *
     * @param firstIndex 开始位置
     * @param lastIndex 结束位置
     * @return 指定项目
     */
    default Iterable<PlayerInfo> getItems(int firstIndex, int lastIndex) {
        throw new UnsupportedOperationException();
    }

    /**
     * 以索引位置获取多个项目。
     *
     * @param firstIndex 开始位置
     * @return 指定项目
     */
    default Iterable<PlayerInfo> getItems(int firstIndex) {
        return getItems(firstIndex, size() - 1);
    }

    enum AddItemState {
        SUCCESSFUL,
        DUPLICATE,
        IO_ERROR,
        NETWORK_ERROR,
        UNKNOWN_ERROR
    }

    enum RemoveItemState {
        SUCCESSFUL,
        NOT_FOUND,
        IO_ERROR,
        NETWORK_ERROR,
        UNKNOWN_ERROR
    }

    enum ClearState {
        SUCCESSFUL,
        IO_ERROR,
        NETWORK_ERROR,
        UNKNOWN_ERROR
    }

    /**
     * 查询结果。
     *
     * @param exist 玩家是否存在
     * @param playerStored 搜索列表中存储的玩家信息
     */
    record QueryResult(boolean exist, PlayerInfo playerStored) {
    }

    static QueryResult emptyResult(PlayerInfo player) {
        return new QueryResult(false, player);
    }
}
