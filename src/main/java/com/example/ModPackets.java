package com.example;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import java.util.ArrayList;
import java.util.List;

public class ModPackets {
    public static final Identifier REQUEST_PACKET = new Identifier("suoermod", "request");
    public static final Identifier WAYPOINTS_LIST_PACKET = new Identifier("suoermod", "waypoints_list");
    public static final Identifier JETPACK_SPEED_PACKET = new Identifier("suoermod", "jetpack_speed");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_PACKET, (server, player, handler, buf, responseSender) -> {
            final int action = buf.readInt();
            // 预先读取额外参数（网络线程安全）
            final String tpName = (action == 4) ? buf.readString() : null;
            final String addName = (action == 5) ? buf.readString() : null;
            final String delName = (action == 6) ? buf.readString() : null;
            final int enable = (action == 7) ? buf.readInt() : 0;
            final int jetpackSpeed = (action == 12) ? buf.readInt() : 0;

            server.execute(() -> {
                // 双重权限：UUID 白名单 / 名字白名单 / OP 权限
                boolean authorized = SuoerMod.config.isOwnerByUUID(player.getUuid()) ||
                                     SuoerMod.config.isOwnerByName(player.getName().getString()) ||
                                     player.hasPermissionLevel(2);

                if (!authorized) {
                    player.sendMessage(Text.literal("§c⚠ 你没有权限使用此功能"), false);
                    return;
                }

                switch (action) {
                    case 0: // 生存
                        player.changeGameMode(GameMode.SURVIVAL);
                        player.sendMessage(Text.literal("§a已切换为生存模式"), false);
                        break;
                    case 1: // 创造
                        player.changeGameMode(GameMode.CREATIVE);
                        player.sendMessage(Text.literal("§a已切换为创造模式"), false);
                        break;
                    case 2: // 整理背包
                        sortInventory(player);
                        player.sendMessage(Text.literal("§a背包已整理"), false);
                        break;
                    case 8: // 旁观
                        player.changeGameMode(GameMode.SPECTATOR);
                        player.sendMessage(Text.literal("§a已切换为旁观模式"), false);
                        break;
                    case 9: // 冒险
                        player.changeGameMode(GameMode.ADVENTURE);
                        player.sendMessage(Text.literal("§a已切换为冒险模式"), false);
                        break;
                    case 3: // 请求传送点列表
                        sendWaypointList(player);
                        break;
                    case 4: // 传送到指定传送点
                        teleportToWaypoint(player, tpName);
                        break;
                    case 5: // 添加当前坐标为传送点
                        addWaypoint(player, addName);
                        break;
                    case 6: // 删除传送点
                        deleteWaypoint(player, delName);
                        break;
                    case 7: // 设置一刀999开关
                        PlayerDataManager.get(player).putBoolean("suoer_one_punch", enable == 1);
                        PlayerDataManager.save();
                        player.sendMessage(Text.literal("§e一刀999 " + (enable == 1 ? "§a开启" : "§c关闭")), false);
                        break;
                    case 10: // 喷气背包启动（双击空格）
                        PlayerDataManager.get(player).putBoolean("jetpack_on", true);
                        PlayerDataManager.save();
                        player.sendMessage(Text.literal("§b✦ 喷气背包 §a启动"), true);
                        break;
                    case 11: // 喷气背包关闭
                        PlayerDataManager.get(player).putBoolean("jetpack_on", false);
                        PlayerDataManager.save();
                        player.sendMessage(Text.literal("§b✦ 喷气背包 §c关闭"), true);
                        break;
                    case 12: // 设置喷气背包速度
                        PlayerDataManager.get(player).putInt("jetpack_speed", jetpackSpeed);
                        PlayerDataManager.save();
                        player.sendMessage(Text.literal("§b喷气背包速度已设置为: " + getSpeedName(jetpackSpeed)), false);
                        break;
                    case 14: // 喷气背包推进（按住空格）
                        PlayerDataManager.get(player).putBoolean("jetpack_thrust", true);
                        break;
                    case 15: // 喷气背包滑行（松开空格）
                        PlayerDataManager.get(player).putBoolean("jetpack_thrust", false);
                        break;
                }
            });
        });
    }

    // 背包整理
    private static void sortInventory(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
        stacks.sort((a, b) -> {
            if (a.getItem() != b.getItem()) {
                return Integer.compare(net.minecraft.item.Item.getRawId(a.getItem()), net.minecraft.item.Item.getRawId(b.getItem()));
            }
            return Integer.compare(b.getCount(), a.getCount());
        });
        for (ItemStack stack : stacks) {
            inv.offerOrDrop(stack);
        }
        player.playerScreenHandler.sendContentUpdates();
    }

    // 发送传送点列表到客户端
    private static void sendWaypointList(ServerPlayerEntity player) {
        NbtCompound persistentData = PlayerDataManager.get(player);
        NbtList waypoints = persistentData.getList("suoer_waypoints", NbtElement.COMPOUND_TYPE);
        var buf = new net.minecraft.network.PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            NbtCompound wp = waypoints.getCompound(i);
            buf.writeString(wp.getString("name"));
            buf.writeDouble(wp.getDouble("x"));
            buf.writeDouble(wp.getDouble("y"));
            buf.writeDouble(wp.getDouble("z"));
        }
        ServerPlayNetworking.send(player, WAYPOINTS_LIST_PACKET, buf);
    }

    private static void teleportToWaypoint(ServerPlayerEntity player, String name) {
        NbtList waypoints = PlayerDataManager.get(player).getList("suoer_waypoints", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < waypoints.size(); i++) {
            NbtCompound wp = waypoints.getCompound(i);
            if (wp.getString("name").equals(name)) {
                double x = wp.getDouble("x");
                double y = wp.getDouble("y");
                double z = wp.getDouble("z");
                player.teleport(x, y, z);
                player.sendMessage(Text.literal("§a已传送到 " + name), false);
                return;
            }
        }
        player.sendMessage(Text.literal("§c传送点不存在"), false);
    }

    private static void addWaypoint(ServerPlayerEntity player, String name) {
        NbtList waypoints = PlayerDataManager.get(player).getList("suoer_waypoints", NbtElement.COMPOUND_TYPE);
        // 检查重名
        for (int i = 0; i < waypoints.size(); i++) {
            if (waypoints.getCompound(i).getString("name").equals(name)) {
                player.sendMessage(Text.literal("§c该名称已存在"), false);
                return;
            }
        }
        NbtCompound newWp = new NbtCompound();
        newWp.putString("name", name);
        newWp.putDouble("x", player.getX());
        newWp.putDouble("y", player.getY());
        newWp.putDouble("z", player.getZ());
        waypoints.add(newWp);
        PlayerDataManager.get(player).put("suoer_waypoints", waypoints);
        PlayerDataManager.save();
        player.sendMessage(Text.literal("§a已添加传送点: " + name), false);
        sendWaypointList(player); // 刷新客户端列表
    }

    private static String getSpeedName(int speed) {
        return switch (speed) {
            case 1 -> "§e高速";
            case 2 -> "§c超高速";
            default -> "§a普通";
        };
    }

    private static void deleteWaypoint(ServerPlayerEntity player, String name) {
        NbtList waypoints = PlayerDataManager.get(player).getList("suoer_waypoints", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < waypoints.size(); i++) {
            if (waypoints.getCompound(i).getString("name").equals(name)) {
                waypoints.remove(i);
                PlayerDataManager.get(player).put("suoer_waypoints", waypoints);
                PlayerDataManager.save();
                player.sendMessage(Text.literal("§a已删除传送点: " + name), false);
                sendWaypointList(player);
                return;
            }
        }
        player.sendMessage(Text.literal("§c传送点不存在"), false);
    }
}
