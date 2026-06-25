package com.example.client;

import com.example.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.text.Text;

public class GameModeScreen extends Screen {
    protected GameModeScreen() {
        super(Text.literal("模式切换"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 30;
        int width = 100;
        int gap = 25;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("生存"), btn -> sendAction(0)).dimensions(centerX - width/2, startY, width, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("创造"), btn -> sendAction(1)).dimensions(centerX - width/2, startY + gap, width, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("旁观"), btn -> sendAction(8)).dimensions(centerX - width/2, startY + gap*2, width, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("冒险"), btn -> sendAction(9)).dimensions(centerX - width/2, startY + gap*3, width, 20).build());
    }

    private void sendAction(int action) {
        if (this.client != null && this.client.player != null) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(action);
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