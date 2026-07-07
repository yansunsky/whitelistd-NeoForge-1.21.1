package com.yansunsky.whitelistd;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

/**
 * Whitelistd 消息与日志辅助工具。
 */
public final class MessageHelper {
    private MessageHelper() {
    }

    /**
     * 向指定目标发送带 Whitelistd 前缀的系统消息。
     *
     * @param target 接收目标
     * @param message 消息内容
     */
    public static void sendSystemMessage(CommandSource target, Component message) {
        target.sendSystemMessage(Component.empty()
                .append(Component.empty().withStyle(ChatFormatting.DARK_AQUA).append("Whitelistd: "))
                .append(message));
    }

    /**
     * 记录调试日志。
     *
     * @param message 日志内容
     */
    public static void sendLogD(String message) {
        Whitelistd.LOGGER.debug(message);
    }

    /**
     * 记录信息日志。
     *
     * @param message 日志内容
     */
    public static void sendLogI(String message) {
        Whitelistd.LOGGER.info(message);
    }

    /**
     * 记录警告日志。
     *
     * @param message 日志内容
     */
    public static void sendLogW(String message) {
        Whitelistd.LOGGER.warn(message);
    }

    /**
     * 记录错误日志。
     *
     * @param message 日志内容
     */
    public static void sendLogE(String message) {
        Whitelistd.LOGGER.error(message);
    }

    /**
     * 记录带异常堆栈的错误日志。
     *
     * @param message 日志内容
     * @param throwable 异常
     */
    public static void sendLogE(String message, Throwable throwable) {
        Whitelistd.LOGGER.error(message, throwable);
    }
}
