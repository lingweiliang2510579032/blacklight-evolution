package com.codex.blacklight.command;

import com.codex.blacklight.power.ClawUpgrade;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.state.BlacklightState;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class BlacklightCommands {
    private BlacklightCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("blacklight")
                .then(Commands.literal("infect").requires(source -> source.hasPermission(2)).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    BlacklightState state = player.getData(ModAttachments.PLAYER_STATE);
                    state.infect();
                    player.setData(ModAttachments.PLAYER_STATE, state);
                    player.syncData(ModAttachments.PLAYER_STATE);
                    context.getSource().sendSuccess(() -> Component.translatable("command.blacklight.infected"), false);
                    return 1;
                }))
                .then(Commands.literal("infected_sample").requires(source -> source.hasPermission(2)).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    var level=player.serverLevel();
                    var zombie=com.codex.blacklight.registry.ModEntities.BLACKLIGHT_HUNTER.get().create(level);
                    if(zombie==null)return 0;
                    var direction=player.getLookAngle().multiply(1,0,1).normalize();
                    var pos=player.position().add(direction.scale(3));
                    zombie.moveTo(pos.x,pos.y,pos.z,player.getYRot()+180,0);
                    zombie.setPersistenceRequired();
                    zombie.setCustomName(Component.literal("黑光感染体 · 重爪"));
                    level.addFreshEntity(zombie);

                    return 1;
                }))
                .then(Commands.literal("form").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                new String[]{"none", "claws", "hammerfists", "blade", "whipfist"}, builder))
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "id");
                            var form = com.codex.blacklight.power.PowerForm.byId(id);
                            if (!form.id().equals(id)) return 0;
                            return com.codex.blacklight.power.PowerFormSystem.select(context.getSource().getPlayerOrException(), form, true) ? 1 : 0;
                        })))
                .then(Commands.literal("status").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    BlacklightState state = player.getData(ModAttachments.PLAYER_STATE);
                    context.getSource().sendSuccess(() -> Component.literal(
                                    "infected=" + state.isInfected() +
                                    " mode=" + (state.isPrototypeMode() ? "prototype" : "vanilla") +
                                    " biomass=" + state.getBiomass() +
                                    " EP=" + state.getEvolutionPoints() +
                                    " power=" + state.getActivePower().id()), false);
                    return 1;
                }))
                .then(Commands.literal("grant_ep").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    BlacklightState state = player.getData(ModAttachments.PLAYER_STATE);
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    state.addEvolutionPoints(amount);
                                    sync(player, state);
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "已获得 " + amount + " EP"), false);
                                    return amount;
                                })))
                .then(Commands.literal("claw_upgrade").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word()).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            BlacklightState state = player.getData(ModAttachments.PLAYER_STATE);
                            String id = StringArgumentType.getString(context, "id");
                            ClawUpgrade upgrade = ClawUpgrade.byId(id);
                            if (upgrade == null) {
                                context.getSource().sendFailure(Component.literal(
                                        "未知升级；可用：range, damage, dashing_slice, pounce"));
                                return 0;
                            }
                            if (!state.buyClawUpgrade(upgrade)) {
                                context.getSource().sendFailure(Component.literal(
                                        "无法购买 " + id + "（需要 " + upgrade.epCost() + " EP，或已经拥有）"));
                                return 0;
                            }
                            sync(player, state);
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "已解锁利爪升级：" + id), false);
                            return 1;
                        }))));
    }

    private static void sync(ServerPlayer player, BlacklightState state) {
        player.setData(ModAttachments.PLAYER_STATE, state);
        player.syncData(ModAttachments.PLAYER_STATE);
    }
}
