package com.mesabrook.rzcraft;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class Config {
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> MODPACK_NAME;
    public static final ForgeConfigSpec.ConfigValue<String> MODPACK_VERSION;
    public static final ForgeConfigSpec.ConfigValue<String> REMOTE_URL;
    public static final ForgeConfigSpec.ConfigValue<String> DOWNLOAD_URL;
    /** auto | json | text */
    public static final ForgeConfigSpec.ConfigValue<String> FORMAT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        // Compact, single section
        b.comment("PackGate client config. Leave fields empty to use built-in fallbacks.")
                .push("general");

        MODPACK_NAME    = b.define("modpackName", "");
        MODPACK_VERSION = b.define("modpackVersion", "");
        REMOTE_URL      = b.define("remoteURL", "");
        DOWNLOAD_URL    = b.define("downloadURL", "");
        FORMAT          = b.comment("Use 'auto' to detect; or force 'json' or 'text'.")
                .define("format", "auto");

        b.pop();
        CLIENT_SPEC = b.build();
    }

    private Config() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "packgate-client.toml");
    }
}
