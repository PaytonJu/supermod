package com.example.mixin;

import com.example.PlayerDataManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    // 一刀999（修改伤害数值）
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float modifyDamage(float originalDamage, DamageSource source) {
        if (source.getAttacker() instanceof ServerPlayerEntity serverPlayer) {
            if (PlayerDataManager.get(serverPlayer).getBoolean("suoer_one_punch")) {
                return 999.0f;
            }
        }
        return originalDamage;
    }

    // 喷气背包飞行无敌 + 落地3秒无敌
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof ServerPlayerEntity player)) return;

        var data = PlayerDataManager.get(player);

        // 飞行中完全无敌
        if (data.getBoolean("jetpack_on")) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // 落地后3秒无敌
        long landTime = data.getLong("jetpack_land_time");
        if (landTime > 0 && System.currentTimeMillis() - landTime < 3000) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
