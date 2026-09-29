package com.hapnoid.autohotbar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "autohotbar_remake.json";

    private static ModConfig config;
    private static volatile int revision = 0;

    /** Bumped on every save, so the client knows to re-run the rules after a config edit. */
    public static int revision() {
        return revision;
    }

    private ConfigManager() {
    }

    public static ModConfig get() {
        if (config == null) {
            load(null);
        }
        return config;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void load(Logger logger) {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, ModConfig.class);
                if (config == null) {
                    config = ModConfig.createDefault();
                }
                return;
            } catch (IOException e) {
                if (logger != null) {
                    logger.warn("Failed to read {}, starting from defaults", FILE_NAME, e);
                }
            }
        }
        config = ModConfig.createDefault();
        save();
    }

    public static void save() {
        revision++;
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save " + FILE_NAME, e);
        }
    }
}
