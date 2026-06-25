package com.example.client;

import com.example.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SuoerModClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;

    // 喷气背包状态
    private static boolean jetpackActive = false;
    private static long lastSpacePressTime = 0;
    private static boolean lastJumpState = false;
    private static final long DOUBLE_TAP_MS = 350;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.suoermod.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,   // ~ 键
                "category.suoermod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 打开菜单
            while (openMenuKey.wasPressed()) {
                client.setScreen(new MainMenuScreen());
            }

            // 喷气背包检测
            if (client.player == null || client.getNetworkHandler() == null) return;

            boolean jumpPressed = client.options.jumpKey.isPressed();
            boolean canJetpack = !client.player.isCreative() && !client.player.isSpectator();

            // 双击空格检测（按下瞬间）
            if (jumpPressed && !lastJumpState && canJetpack) {
                long now = System.currentTimeMillis();
                boolean isInAir = !client.player.isOnGround();

                if (isInAir && now - lastSpacePressTime < DOUBLE_TAP_MS) {
                    // 双击空格：切换喷气背包开关
                    jetpackActive = !jetpackActive;
                    if (jetpackActive) {
                        sendAction(10); // 启动
                        // 启动时自动推进
                        sendAction(14);
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§b✦ 喷气背包 §a启动 §7(空格推进/松开滑翔)"), true);
                        }
                    } else {
                        sendAction(11); // 关闭
                        sendAction(15); // 也停止推进
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§b✦ 喷气背包 §c关闭"), true);
                        }
                    }
                } else if (jetpackActive && !isInAir) {
                    // 落地了但 jetpack 还在 → 让服务端处理，但也标记一下
                    jetpackActive = false;
                }
                lastSpacePressTime = now;
            }

            if (jetpackActive) {
                // 空格按下→推进，松开→滑行
                if (jumpPressed && !lastJumpState) {
                    sendAction(14); // 推进
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§b🔥 推进"), true);
                    }
                } else if (!jumpPressed && lastJumpState) {
                    sendAction(15); // 滑行
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§b🪶 滑行"), true);
                    }
                }
            }

            // 落地时自动标记关闭（防止状态不同步）
            if (jetpackActive && client.player.isOnGround()) {
                jetpackActive = false;
                sendAction(15);
            }

            lastJumpState = jumpPressed;
        });

        // 接收服务端发来的传送点列表
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.WAYPOINTS_LIST_PACKET, (client, handler, buf, responseSender) -> {
            int size = buf.readInt();
            List<Waypoint> waypoints = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String name = buf.readString();
                double x = buf.readDouble();
                double y = buf.readDouble();
                double z = buf.readDouble();
                waypoints.add(new Waypoint(name, x, y, z));
            }
            client.execute(() -> {
                if (client.currentScreen instanceof TeleportScreen) {
                    ((TeleportScreen) client.currentScreen).updateWaypoints(waypoints);
                } else if (!(client.currentScreen instanceof WaypointDetailScreen)) {
                    client.setScreen(new TeleportScreen(waypoints));
                }
            });
        });
    }

    private void sendAction(int action) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(action);
        ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, buf);
    }

    // 传送点数据结构
    public record Waypoint(String name, double x, double y, double z) {}
}
