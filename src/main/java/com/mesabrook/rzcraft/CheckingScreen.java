package com.mesabrook.rzcraft;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CheckingScreen extends Screen {
    private static final Logger LOG = LogManager.getLogger("PackGate/CheckingScreen");

    private final Screen nextIfOk;
    private int dots = 0, tickCounter = 0;

    public CheckingScreen(Screen previousScreen) {
        super(Component.literal("Checking " + prettyPackName() + " Version…"));
        this.nextIfOk = previousScreen;
        LOG.info("Showing CheckingScreen… awaiting version result.");
    }

    private static String prettyPackName() {
        String s = Config.MODPACK_NAME.get();
        return (s == null || s.isBlank()) ? "Modpack" : s.trim();
    }

    @Override
    public void tick() {
        if (++tickCounter % 10 == 0) dots = (dots + 1) % 4;

        var res = VersionCheck.getNowOrNull();
        if (res == null || res.state == VersionCheck.State.RUNNING) return;

        switch (res.state) {
            case OUTDATED -> {
                LOG.info("Result OUTDATED → opening blocker: {}", res);
                minecraft.setScreen(Screens.makeBlocker(res));
            }
            case OK -> {
                LOG.info("Result OK → returning to previous screen.");
                minecraft.setScreen(nextIfOk);
            }
            case ERROR -> {
                LOG.warn("Result ERROR → policy allows play; returning to previous screen.");
                minecraft.setScreen(nextIfOk);
            }
        }
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gg);
        String text = "Checking " + prettyPackName() + " version" + ".".repeat(dots);
        gg.drawCenteredString(this.font, this.getTitle(), this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        gg.drawCenteredString(this.font, text, this.width / 2, this.height / 2, 0xDDDDDD);
        super.render(gg, mouseX, mouseY, partialTick);
    }
}
