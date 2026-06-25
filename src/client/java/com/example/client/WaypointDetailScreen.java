package com.example.client;

import com.example.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.text.Text;

public class WaypointDetailScreen extends Screen {
    private final SuoerModClient.Waypoint waypoint;

    protected WaypointDetailScreen(SuoerModClient.Waypoint waypoint) {
        super(Text.literal("传送点: " + waypoint.name()));
        this.waypoint = waypoint;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        // 传送按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a✦ 传送到此坐标"),
                btn -> {
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeInt(4);
                    buf.writeString(waypoint.name());
                    ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, buf);
                    this.close();
                }
        ).dimensions(centerX - 75, startY, 150, 20).build());

        // 删除按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§c✕ 删除"),
                btn -> {
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeInt(6);
                    buf.writeString(waypoint.name());
                    ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, buf);

                    // 刷新列表
                    PacketByteBuf refreshBuf = new PacketByteBuf(Unpooled.buffer());
                    refreshBuf.writeInt(3);
                    ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, refreshBuf);

                    this.close();
                }
        ).dimensions(centerX - 75, startY + 30, 150, 20).build());

        // 返回按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("返回列表"),
                btn -> {
                    // 请求刷新列表（会重新打开 TeleportScreen）
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeInt(3);
                    ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, buf);
                }
        ).dimensions(centerX - 75, startY + 60, 150, 20).build());

        // 关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("关闭"),
                btn -> this.close()
        ).dimensions(this.width - 60, this.height - 30, 50, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        // 标题：备注名
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§e§l" + waypoint.name()), this.width / 2, 20, 0xFFFF55);

        // 坐标信息
        String coords = String.format("§7坐标: §fX=%.0f  Y=%.0f  Z=%.0f", waypoint.x(), waypoint.y(), waypoint.z());
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(coords), this.width / 2, 45, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
