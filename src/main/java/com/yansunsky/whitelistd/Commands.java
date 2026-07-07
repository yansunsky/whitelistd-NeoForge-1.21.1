package com.yansunsky.whitelistd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;
import java.util.function.Predicate;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Whitelistd 管理指令。
 */
public final class Commands {
    private Commands() {
    }

    /**
     * 注册 Whitelistd 管理指令。
     *
     * @param event NeoForge 指令注册事件
     */
    public static void register(RegisterCommandsEvent event) {
        Whitelistd instance = Whitelistd.getInstance();
        WhitelistdConfig config = instance.getConfig();
        Predicate<CommandSourceStack> require = source -> source.hasPermission(config.getPermissionLevel());
        SuggestionProvider<CommandSourceStack> nameSuggest = (context, builder) -> {
            for (var player : context.getSource().getServer().getPlayerList().getPlayers()) {
                String name = player.getName().getString();
                String uuid = player.getStringUUID();
                builder.suggest(name);
                builder.suggest(name + ' ' + uuid);
            }
            return builder.buildFuture();
        };

        var string = StringArgumentType.string();
        var dispatcher = event.getDispatcher();
        var whitelistd = dispatcher.register(literal("whitelistd")
                .requires(require)
                .then(literal("add")
                        .then(argument("name", string)
                                .suggests(nameSuggest)
                                .executes(context -> whitelistdCommand(Operation.ADD, false, context))
                                .then(argument("uuid", string)
                                        .executes(context -> whitelistdCommand(Operation.ADD, true, context))
                                )
                        )
                )
                .then(literal("remove")
                        .then(argument("name", string)
                                .suggests(nameSuggest)
                                .executes(context -> whitelistdCommand(Operation.REMOVE, false, context))
                                .then(argument("uuid", string)
                                        .executes(context -> whitelistdCommand(Operation.REMOVE, true, context))
                                )
                        )
                )
                .then(literal("query")
                        .then(argument("name", string)
                                .suggests(nameSuggest)
                                .executes(context -> whitelistdCommand(Operation.QUERY, false, context))
                                .then(argument("uuid", string)
                                        .executes(context -> whitelistdCommand(Operation.QUERY, true, context))
                                )
                        )
                )
                .then(literal("on")
                        .executes(context -> {
                            instance.setEnabled(true);
                            CommandSourceStack source = context.getSource();
                            source.sendSystemMessage(Component.translatable("wld.status.enable_whitelist"));
                            MessageHelper.sendLogI(Component.translatable("wld.console.enable_whitelist", source.getTextName()).getString());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(literal("off")
                        .executes(context -> {
                            instance.setEnabled(false);
                            CommandSourceStack source = context.getSource();
                            source.sendSystemMessage(Component.translatable("wld.status.disable_whitelist"));
                            MessageHelper.sendLogW(Component.translatable("wld.console.disable_whitelist", source.getTextName()).getString());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(literal("list")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            for (PlayerInfo info : instance.getSearchList().getItems()) {
                                source.sendSystemMessage(Component.literal(info.getName() + '{' + info.getUuid() + '}'));
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
        dispatcher.register(literal("wld").requires(require).redirect(whitelistd));

        if (config.isEnableRecord()) {
            dispatcher.register(literal("record")
                    .requires(require)
                    .then(argument("name", string)
                            .executes(Commands::recordCommand)
                    )
            );
        }
    }

    private static int recordCommand(CommandContext<CommandSourceStack> context) {
        String name = context.getArgument("name", String.class);
        SearchList searchList = Whitelistd.getInstance().getSearchList();
        CommandSourceStack source = context.getSource();
        SearchList.QueryResult result = searchList.query(new PlayerInfo(name));
        if (result.exist()) {
            source.sendFailure(Component.translatable("wld.status.duplicated_name"));
            return Command.SINGLE_SUCCESS;
        }

        SearchList.AddItemState state = searchList.addItem(new PlayerInfo(name + ".record"));
        if (state == SearchList.AddItemState.SUCCESSFUL) {
            source.sendSystemMessage(Component.translatable("wld.status.add_record"));
            MessageHelper.sendLogI(Component.translatable("wld.console.add_record", source.getTextName(), name).getString());
        } else {
            source.sendFailure(Component.translatable("wld.status.failed", state.toString()));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int whitelistdCommand(Operation operation, boolean existUuid, CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String name = context.getArgument("name", String.class);
            String uuid = existUuid ? context.getArgument("uuid", String.class) : null;
            PlayerInfo playerInfo = new PlayerInfo(name, uuid == null ? null : UUID.fromString(uuid));
            SearchList searchList = Whitelistd.getInstance().getSearchList();
            String profileName = name + '{' + uuid + '}';

            switch (operation) {
                case ADD -> addWhitelistItem(source, searchList, playerInfo, profileName);
                case REMOVE -> removeWhitelistItem(source, searchList, playerInfo, profileName);
                case QUERY -> queryWhitelistItem(source, searchList, playerInfo);
            }
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.translatable("wld.status.illegal_uuid_format"));
        } catch (Exception e) {
            MessageHelper.sendLogE(Component.translatable("wld.console.unexpected_error").getString(), e);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void addWhitelistItem(CommandSourceStack source, SearchList searchList, PlayerInfo playerInfo, String profileName) {
        SearchList.AddItemState state = searchList.addItem(playerInfo);
        if (state == SearchList.AddItemState.SUCCESSFUL) {
            source.sendSystemMessage(Component.translatable("wld.status.add_whitelist_item"));
            MessageHelper.sendLogI(Component.translatable("wld.console.add_whitelist_item", source.getTextName(), profileName).getString());
        } else {
            source.sendFailure(Component.translatable("wld.status.failed", state.toString()));
        }
    }

    private static void removeWhitelistItem(CommandSourceStack source, SearchList searchList, PlayerInfo playerInfo, String profileName) {
        SearchList.RemoveItemState state = searchList.removeItem(playerInfo);
        if (state == SearchList.RemoveItemState.SUCCESSFUL) {
            source.sendSystemMessage(Component.translatable("wld.status.remove_whitelist_item"));
            MessageHelper.sendLogI(Component.translatable("wld.console.remove_whitelist_item", source.getTextName(), profileName).getString());
        } else {
            source.sendFailure(Component.translatable("wld.status.failed", state.toString()));
        }
    }

    private static void queryWhitelistItem(CommandSourceStack source, SearchList searchList, PlayerInfo playerInfo) {
        SearchList.QueryResult result = searchList.query(playerInfo);
        if (result.exist()) {
            PlayerInfo stored = result.playerStored();
            source.sendSystemMessage(Component.translatable("wld.status.found", stored.getName() + '{' + stored.getUuid() + '}'));
        } else {
            source.sendSystemMessage(Component.translatable("wld.status.not_found"));
        }
    }

    private enum Operation {
        ADD,
        REMOVE,
        QUERY
    }
}
