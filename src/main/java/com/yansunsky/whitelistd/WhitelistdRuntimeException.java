package com.yansunsky.whitelistd;

/**
 * Whitelistd 初始化或运行过程中的不可恢复异常。
 */
public class WhitelistdRuntimeException extends RuntimeException {
    public WhitelistdRuntimeException(String message) {
        super(message);
    }

    public WhitelistdRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
