package com.example.client;

import com.example.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.text.Text;

public class JetpackScreen extends Screen {
    private int currentSpeed; // 0=普通, 1=高速, 2=超高速
    private ButtonWidget toggleBtn;
    private ButtonWidget speedBtn;

    protected JetpackScreen(int currentSpeed) {
        super(Text.literal("喷气背包"));
        this.currentSpeed = currentSpeed;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 30;

        // 速度切换按钮
        speedBtn = ButtonWidget.builder(
                getSpeedText(),
                btn -> {
                    currentSpeed = (currentSpeed + 1) % 3;
                    speedBtn.setMessage(getSpeedText());
                    // 保存到服务端
                    saveSpeed();
                }
        ).dimensions(centerX - 75, startY, 150, 20).build();
        this.addDrawableChild(speedBtn);

        // 提示说明
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("双击空格启动"),
                btn -> {}
        ).dimensions(centerX - 75, startY + 30, 150, 20).build()).active = false;

        // 关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("关闭"),
                btn -> this.close()
        ).dimensions(centerX - 50, startY + 60, 100, 20).build());
    }

    private Text getSpeedText() {
        return switch (currentSpeed) {
            case 0 -> Text.literal("§a速度: 普通");
            case 1 -> Text.literal("§e速度: 高速");
            case 2 -> Text.literal("§c速度: 超高速");
            default -> Text.literal("速度: 普通");
        };
    }

    private void saveSpeed() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(12); // action 12 = 设置喷气背包速度
        buf.writeInt(currentSpeed);
        ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§b✦ 喷气背包"), this.width / 2, 15, 0x55FFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("在空中双击空格启动，不消耗任何资源"), this.width / 2, this.height / 2 - 55, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
