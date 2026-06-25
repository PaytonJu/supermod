package com.example.client;

import com.example.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {
    protected MainMenuScreen() {
        super(Text.literal("SuoerMod 面板"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 60;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int gap = 25;

        // 左列
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("模式切换"),
                btn -> this.client.setScreen(new GameModeScreen())
        ).dimensions(centerX - 110, startY, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("整理背包"),
                btn -> sendRequest(2)
        ).dimensions(centerX - 110, startY + gap, buttonWidth, buttonHeight).build());

        // 右列
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("传送点"),
                btn -> sendRequest(3)
        ).dimensions(centerX + 10, startY, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("一刀 999"),
                btn -> this.client.setScreen(new OnePunchScreen())
        ).dimensions(centerX + 10, startY + gap, buttonWidth, buttonHeight).build());

        // 中间：喷气背包
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§b✦ 喷气背包"),
                btn -> openJetpackScreen()
        ).dimensions(centerX - 50, startY + gap * 2 + 5, 100, 20).build());
    }

    private void openJetpackScreen() {
        // 先请求当前速度设置，再打开界面
        // 简化处理：先打开，默认速度 0
        this.client.setScreen(new JetpackScreen(0));
    }

    private void sendRequest(int action) {
        if (this.client != null && this.client.player != null) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(action);
            ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, buf);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }
}
