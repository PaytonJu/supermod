package com.example.client;

import com.example.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.text.Text;

public class AddWaypointScreen extends Screen {
    private TextFieldWidget nameField;
    private ButtonWidget confirmBtn;

    protected AddWaypointScreen() {
        super(Text.literal("添加传送点"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        nameField = new TextFieldWidget(this.textRenderer, centerX - 100, this.height / 2 - 30, 200, 20, Text.literal("备注名"));
        nameField.setMaxLength(64);
        this.addSelectableChild(nameField);

        confirmBtn = ButtonWidget.builder(
                Text.literal("确认添加"),
                btn -> addWaypoint()
        ).dimensions(centerX - 50, this.height / 2 + 10, 100, 20).build();
        confirmBtn.active = false;
        this.addDrawableChild(confirmBtn);

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("取消"),
                btn -> this.close()
        ).dimensions(centerX - 50, this.height / 2 + 40, 100, 20).build());

        nameField.setChangedListener(text -> confirmBtn.active = !text.trim().isEmpty());
        nameField.setFocused(true);
    }

    private void addWaypoint() {
        String note = nameField.getText().trim();
        if (note.isEmpty()) return;

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(5);
        buf.writeString(note);
        ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, buf);

        // 请求刷新列表
        PacketByteBuf refreshBuf = new PacketByteBuf(Unpooled.buffer());
        refreshBuf.writeInt(3);
        ClientPlayNetworking.send(ModPackets.REQUEST_PACKET, refreshBuf);

        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("输入坐标备注名"), this.width / 2, this.height / 2 - 55, 0xAAAAAA);
        nameField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
