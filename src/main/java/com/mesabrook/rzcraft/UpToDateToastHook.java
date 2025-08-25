package com.mesabrook.rzcraft;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UpToDateToastHook {
    private static final Logger LOG = LogManager.getLogger("PackGate/ToastHook");
    private static boolean attempted = false;  // prevent spamming checks after success

    private UpToDateToastHook() {}

    /** When a screen finishes initializing, try to show the toast if result is OK. */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post e) {
        if (UpToDateToast.isShown()) return;

        var res = VersionCheck.getNowOrNull();
        if (res == null || res.state != VersionCheck.State.OK) return;

        // Avoid showing while our own gating UI is up
        if (e.getScreen() instanceof CheckingScreen || e.getScreen() instanceof VersionBlockerScreen) return;

        String name = Config.MODPACK_NAME.get();
        String pack = (name == null || name.isBlank()) ? "Modpack" : name.trim();
        LOG.info("Screen initialized: {} — showing up-to-date toast.", e.getScreen().getClass().getSimpleName());
        UpToDateToast.show(pack, res.current);
        attempted = true;
    }

    /** Fallback: if for some reason Init didn’t fire after result OK, try once on a later client tick. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || UpToDateToast.isShown() || attempted) return;

        var res = VersionCheck.getNowOrNull();
        if (res == null || res.state != VersionCheck.State.OK) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.screen == null) return;
        if (mc.screen instanceof CheckingScreen || mc.screen instanceof VersionBlockerScreen) return;

        String name = Config.MODPACK_NAME.get();
        String pack = (name == null || name.isBlank()) ? "Modpack" : name.trim();
        LOG.info("ClientTick fallback — current screen={}, showing up-to-date toast.",
                mc.screen.getClass().getSimpleName());
        UpToDateToast.show(pack, res.current);
        attempted = true;
    }
}
