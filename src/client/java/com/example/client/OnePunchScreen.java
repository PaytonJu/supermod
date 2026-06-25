package com.example.client;

import com.example.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.text.Text;

public class OnePunchScreen extends Screen {
    protected OnePunchScreen() {
        super(Text.literal("一刀 999"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 20;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("开启"), btn -> setMode(1)).dimensions(centerX - 50, centerY, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("关闭"), btn -> setMode(0)).dimensions(centerX - 50, centerY + 25, 100, 20).build());
    }

    private void setMode(int enable) {
        if (this.client != null && this.client.player != null) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(7);
            buf.writeInt(enable);
            ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, buf);
            this.close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }
}