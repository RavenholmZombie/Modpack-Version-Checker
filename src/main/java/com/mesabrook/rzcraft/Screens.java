package com.mesabrook.rzcraft;

import net.minecraft.network.chat.Component;

public final class Screens {
    private Screens() {}

    static VersionBlockerScreen makeBlocker(VersionCheck.Result res) {
        String name = Config.MODPACK_NAME.get();
        String pack = (name == null || name.isBlank()) ? "your modpack" : name.trim();

        String msg = pack + " is out of date.\n\n" +
                "Installed: " + res.current + "\n" +
                "Latest: " + res.latest + "\n\n" +
                "Please update " + pack + " to continue.";


         return new VersionBlockerScreen(
                 Component.literal("Update Required — " + pack),
                 Component.literal(msg),
                 res.downloadUrl,
                 res.configDownloadUrl
         );
    }
}
