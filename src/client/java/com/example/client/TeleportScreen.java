package com.example.client;

import com.example.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.text.Text;
import java.util.List;

public class TeleportScreen extends Screen {
    private List<SuoerModClient.Waypoint> waypoints;

    protected TeleportScreen(List<SuoerModClient.Waypoint> waypoints) {
        super(Text.literal("传送点管理"));
        this.waypoints = waypoints;
    }

    public void updateWaypoints(List<SuoerModClient.Waypoint> newWaypoints) {
        this.waypoints = newWaypoints;
        clearAndInit();
    }

    @Override
    protected void init() {
        int yOffset = 40;
        int index = 0;

        if (waypoints != null) {
            for (SuoerModClient.Waypoint wp : waypoints) {
                // 只显示备注名，点击进入详情
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("§6✦ " + wp.name()),
                        btn -> this.client.setScreen(new WaypointDetailScreen(wp))
                ).dimensions(10, yOffset + index * 25, 200, 20).build());

                // 快速删除按钮
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("§c✕"),
                        btn -> {
                            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                            buf.writeInt(6);
                            buf.writeString(wp.name());
                            ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, buf);
                            // 请求刷新
                            PacketByteBuf refreshBuf = new PacketByteBuf(Unpooled.buffer());
                            refreshBuf.writeInt(3);
                            ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, refreshBuf);
                        }
                ).dimensions(215, yOffset + index * 25, 30, 20).build());

                index++;
            }
        }

        // 添加当前坐标按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a+ 添加当前坐标"),
                btn -> this.client.setScreen(new AddWaypointScreen())
        ).dimensions(10, yOffset + index * 25 + 5, 150, 20).build());

        // 刷新按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§7⟳ 刷新"),
                btn -> {
                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeInt(3);
                    ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, buf);
                }
        ).dimensions(165, yOffset + index * 25 + 5, 50, 20).build());

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
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }
}
