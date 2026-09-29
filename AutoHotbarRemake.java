package com.hapnoid.autohotbar;

import com.hapnoid.autohotbar.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoHotbarRemake implements ModInitializer {
    public static final String MOD_ID = "autohotbar_remake";
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoHotbar Remake");

    @Override
    public void onInitialize() {
        ConfigManager.load(LOGGER);
        LOGGER.info("AutoHotbar Remake loaded ({} slot configs)", ConfigManager.get().slots.size());
    }
}
