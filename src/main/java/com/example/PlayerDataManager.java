package com.example;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家持久化数据管理器
 * Fabric 1.20.1 没有内置的 getPersistentData()，所以手动用文件存
 */
public class PlayerDataManager {
    private static final Map<UUID, NbtCompound> dataMap = new HashMap<>();
    private static File dataFile = null;

    public static void init(MinecraftServer server) {
        // 保存到 config/suoermod/playerdata.dat（不依赖世界名，gradlew clean 也不会删）
        dataFile = new File(new File(server.getRunDirectory(), "config/suoermod"), "playerdata.dat");

        if (dataFile.exists()) {
            try {
                NbtCompound root = NbtIo.readCompressed(dataFile);
                if (root != null) {
                    NbtList list = root.getList("players", NbtElement.COMPOUND_TYPE);
                    for (int i = 0; i < list.size(); i++) {
                        NbtCompound entry = list.getCompound(i);
                        UUID uuid = entry.getUuid("uuid");
                        NbtCompound data = entry.getCompound("data");
                        dataMap.put(uuid, data);
                    }
                }
                SuoerMod.LOGGER.info("PlayerDataManager: 已加载 {} 个玩家的数据", dataMap.size());
            } catch (IOException e) {
                SuoerMod.LOGGER.error("PlayerDataManager: 读取数据失败", e);
            }
        }
    }

    public static void save() {
        if (dataFile == null) return;
        NbtCompound root = new NbtCompound();
        NbtList list = new NbtList();
        for (Map.Entry<UUID, NbtCompound> entry : dataMap.entrySet()) {
            NbtCompound item = new NbtCompound();
            item.putUuid("uuid", entry.getKey());
            item.put("data", entry.getValue());
            list.add(item);
        }
        root.put("players", list);
        try {
            dataFile.getParentFile().mkdirs();
            NbtIo.writeCompressed(root, dataFile);
        } catch (IOException e) {
            SuoerMod.LOGGER.error("PlayerDataManager: 保存数据失败", e);
        }
    }

    public static NbtCompound get(ServerPlayerEntity player) {
        return dataMap.computeIfAbsent(player.getUuid(), k -> new NbtCompound());
    }
}
