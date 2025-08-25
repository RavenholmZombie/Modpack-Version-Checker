package com.mesabrook.rzcraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class UpToDateToast {
    private static final Logger LOG = LogManager.getLogger("PackGate/Toast");
    private static boolean shown = false;

    private UpToDateToast() {}

    public static boolean isShown() { return shown; }

    public static void show(String packName, String version) {
        if (shown) {
            LOG.debug("Toast already shown; skipping.");
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            LOG.warn("Minecraft instance null; cannot show toast.");
            return;
        }

        String pack = (packName == null || packName.isBlank()) ? "Modpack" : packName.trim();
        Component title = Component.literal(pack + " is up to date");
        Component desc  = Component.literal("Installed version: " + (version == null || version.isBlank() ? "unknown" : version));

        SystemToast.add(mc.getToasts(), SystemToast.SystemToastIds.TUTORIAL_HINT, title, desc);
        shown = true;
        LOG.info("Displayed 'up-to-date' toast: pack='{}', version='{}'", pack, version);
    }

    // Optional: for dev testing if you want to see the toast again without restarting
    public static void resetForDebug() { shown = false; }
}
