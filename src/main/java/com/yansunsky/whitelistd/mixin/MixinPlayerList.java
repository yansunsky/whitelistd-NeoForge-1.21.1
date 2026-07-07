package com.yansunsky.whitelistd.mixin;

import com.mojang.authlib.GameProfile;
import com.yansunsky.whitelistd.MessageHelper;
import com.yansunsky.whitelistd.PlayerInfo;
import com.yansunsky.whitelistd.SearchList;
import com.yansunsky.whitelistd.WhitelistHelper;
import com.yansunsky.whitelistd.Whitelistd;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 接管原版专用服务器白名单判断。
 */
@Mixin(DedicatedPlayerList.class)
public abstract class MixinPlayerList {
    /**
     * 在原版白名单判断入口处优先使用 Whitelistd 查询结果。
     *
     * @param profile 玩家档案
     * @param callback 回调返回值
     */
    @Inject(at = @At("HEAD"), method = "isWhiteListed(Lcom/mojang/authlib/GameProfile;)Z", cancellable = true)
    private void whitelistd$isWhiteListed(GameProfile profile, CallbackInfoReturnable<Boolean> callback) {
        Whitelistd instance = Whitelistd.getInstance();
        if (!instance.isEnabled() || !instance.isReady()) {
            return;
        }

        String name = profile.getName();
        String profileString = name + '{' + profile.getId() + '}';
        MessageHelper.sendLogI("Whitelist request: " + profileString);

        if (instance.isAllowAll()) {
            MessageHelper.sendLogI("Allowed " + name + " (allow-all mode)");
            callback.setReturnValue(true);
            return;
        }

        if (name == null) {
            MessageHelper.sendLogI("Rejected (name is null)");
            callback.setReturnValue(false);
            return;
        }

        SearchList.QueryResult result = WhitelistHelper.query(new PlayerInfo(name, profile.getId()));
        boolean allowed = result.exist();
        MessageHelper.sendLogI((allowed ? "Allowed " : "Rejected ") + name);
        callback.setReturnValue(allowed);
    }
}
