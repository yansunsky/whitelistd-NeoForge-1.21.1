package com.yansunsky.whitelistd;

import net.minecraft.network.chat.Component;

/**
 * 白名单查询工具。
 */
public final class WhitelistHelper {
    private WhitelistHelper() {
    }

    /**
     * 查询玩家是否在白名单内，并处理 Record 占位转换。
     *
     * @param player 玩家信息
     * @return 查询结果
     */
    public static SearchList.QueryResult query(PlayerInfo player) {
        Whitelistd instance = Whitelistd.getInstance();
        if (!instance.isReady()) {
            return SearchList.emptyResult(player);
        }

        WhitelistdConfig config = instance.getConfig();
        SearchList searchList = instance.getSearchList();
        String name = player.getName();
        if (config.getSearchMode() == SearchMode.PLAYER_UUID && player.getUuid() == null) {
            return SearchList.emptyResult(player);
        }

        SearchList.QueryResult result = searchList.query(player);
        if (result.exist() || !config.isEnableRecord() || player.getUuid() == null) {
            return result;
        }

        String recordName = name + ".record";
        PlayerInfo recordPlayer = new PlayerInfo(recordName);
        SearchList.QueryResult recordResult = searchList.query(recordPlayer);
        if (!recordResult.exist()) {
            return result;
        }

        SearchList.AddItemState addState = searchList.addItem(player);
        if (addState != SearchList.AddItemState.SUCCESSFUL) {
            MessageHelper.sendLogE(Component.translatable("wld.console.record_add_failed", addState.toString()).getString());
            return result;
        }

        MessageHelper.sendLogI(Component.translatable("wld.console.record_hit", name).getString());
        SearchList.RemoveItemState removeState = searchList.removeItem(recordPlayer);
        if (removeState != SearchList.RemoveItemState.SUCCESSFUL) {
            MessageHelper.sendLogE(Component.translatable("wld.console.record_remove_failed", removeState.toString()).getString());
            MessageHelper.sendLogE(Component.translatable("wld.console.record_remove_manually_hint", recordName).getString());
        }
        return new SearchList.QueryResult(true, player);
    }
}
