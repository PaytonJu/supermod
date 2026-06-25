package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;
    public List<String> ownerUUIDs = new ArrayList<>();
    public List<String> ownerNames = new ArrayList<>();

    public static ModConfig load() {
        configFile = FabricLoader.getInstance().getConfigDir().resolve("suoermod.json").toFile();
        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                return GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    public void save() {
        try (Writer writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isOwnerByUUID(UUID uuid) {
        return ownerUUIDs.contains(uuid.toString());
    }

    public boolean isOwnerByName(String name) {
        return ownerNames.contains(name);
    }
}