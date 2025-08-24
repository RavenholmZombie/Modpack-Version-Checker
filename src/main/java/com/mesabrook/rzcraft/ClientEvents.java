package com.mesabrook.rzcraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents
{
    private static boolean handledTitleOnce = false;
    private ClientEvents() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event)
    {
        if (handledTitleOnce) return;

        if (event.getNewScreen() instanceof TitleScreen)
        {
            var res = VersionCheck.getNowOrNull();
            if (res == null || res.state == VersionCheck.State.RUNNING) {
                // Swap to a small waiting screen until the check completes.
                event.setNewScreen(new CheckingScreen((TitleScreen) event.getNewScreen()));
                handledTitleOnce = true;
                return;
            }

            if (res.state == VersionCheck.State.OUTDATED) {
                event.setNewScreen(Screens.makeBlocker(res));
                handledTitleOnce = true;
            } else {
                // OK or ERROR (your policy: allow play on ERROR; change if you want fail-closed)
                handledTitleOnce = false;
            }
        }
    }
}
