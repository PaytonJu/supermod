package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.PlayerInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.minecraft.server.command.CommandManager.*;

public class SuoerMod implements ModInitializer {
    public static final String MOD_ID = "supermod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig config;

    @Override
    public void onInitialize() {
        config = ModConfig.load();
        ModPackets.registerServerReceivers();

        // 玩家断线时自动关喷气背包
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerDataManager.get(handler.player).putBoolean("jetpack_on", false);
        });

        // 初始化玩家数据管理器
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlayerDataManager.init(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PlayerDataManager.save();
        });

        // 进入世界时显示 UUID 和名字
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String uuid = handler.player.getUuid().toString();
            String name = handler.player.getName().getString();
            handler.player.sendMessage(Text.literal("§e你的 UUID: §f" + uuid + "  名字: " + name), false);
        });

        // 注册 /suoer addme 命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("super")
                .then(literal("addme")
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayer();
                        String uuid = player.getUuid().toString();
                        String name = player.getName().getString();
                        boolean changed = false;

                        if (!config.ownerUUIDs.contains(uuid)) {
                            config.ownerUUIDs.add(uuid);
                            changed = true;
                        }
                        if (!config.ownerNames.contains(name)) {
                            config.ownerNames.add(name);
                            changed = true;
                        }
                        if (changed) {
                            config.save();
                            ctx.getSource().sendFeedback(() -> Text.literal("§a已将你添加到 SuperMod 主人列表（UUID 和名字）"), false);
                        } else {
                            ctx.getSource().sendFeedback(() -> Text.literal("§e你已经拥有权限"), false);
                        }
                        return 1;
                    })
                )
            );
        });

        // 喷气背包（鞘翅式飞行：朝视角方向飞，空格推进，松开滑翔）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                var data = PlayerDataManager.get(player);
                if (!data.getBoolean("jetpack_on")) {
                    // 不在飞行中，检查落地无敌计时
                    long landTime = data.getLong("jetpack_land_time");
                    if (landTime > 0 && now - landTime > 3000) {
                        data.remove("jetpack_land_time");
                    }
                    continue;
                }

                player.fallDistance = 0;

                if (player.isOnGround()) {
                    // 落地 → 自动关闭，记录落地时间用于无敌
                    data.putLong("jetpack_land_time", now);
                    data.putBoolean("jetpack_on", false);
                    player.sendMessage(Text.literal("§b✦ 喷气背包 §c关闭（已落地）"), true);
                    continue;
                }

                if (data.getBoolean("jetpack_thrust")) {
                    // 推进模式：朝视角方向飞
                    Vec3d look = player.getRotationVector();
                    int speedLevel = data.getInt("jetpack_speed");
                    double speed = switch (speedLevel) {
                        case 1 -> 1.5;  // 高速
                        case 2 -> 2.5;  // 超高速
                        default -> 0.8; // 普通
                    };
                    player.setVelocity(look.x * speed, look.y * speed, look.z * speed);
                    player.velocityModified = true;
                } else {
                    // 滑行模式：保持水平速度，缓慢下降
                    Vec3d vel = player.getVelocity();
                    double horizontal = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                    if (horizontal > 0.1) {
                        // 缓降，像鞘翅
                        if (vel.y < -0.3) {
                            player.setVelocity(vel.x, -0.2, vel.z);
                            player.velocityModified = true;
                        }
                    }
                }
            }
        });

        // 自动替换损坏工具（每 10 tick ≈ 0.5 秒检查一次，避免每 tick 遍历）
        ServerTickEvents.END_SERVER_TICK.register(new net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick() {
            private int tickCounter = 0;

            @Override
            public void onEndTick(net.minecraft.server.MinecraftServer server) {
                if (++tickCounter < 10) return;
                tickCounter = 0;

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    ItemStack mainHand = player.getMainHandStack();
                    if (!mainHand.isDamageable() || mainHand.getDamage() < mainHand.getMaxDamage() - 3) continue;
                    PlayerInventory inv = player.getInventory();
                    for (int i = 0; i < inv.main.size(); i++) {
                        ItemStack stack = inv.main.get(i);
                        if (stack.isDamageable() && stack.getItem() == mainHand.getItem() && stack.getDamage() < mainHand.getDamage()) {
                            inv.main.set(i, mainHand.copy());
                            player.setStackInHand(Hand.MAIN_HAND, stack);
                            break;
                        }
                    }
                }
            }
        });

        LOGGER.info("SuperMod 已加载");
    }
}