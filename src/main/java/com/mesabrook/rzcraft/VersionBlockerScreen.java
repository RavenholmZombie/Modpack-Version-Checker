package com.mesabrook.rzcraft;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.net.URI;
import java.util.List;

public class VersionBlockerScreen extends Screen
{
    private final Component message;
    private final String downloadUrl;
    private final String packDownloadUrl;

    // cached layout
    private List<FormattedCharSequence> wrapped;
    private int titleY;
    private int messageY;
    private int openBtnY;
    private int quitBtnY;
    private int maxWidth;

    public VersionBlockerScreen(Component title, Component message, String downloadUrl, String packDownloadUrl)
    {
        super(title);
        this.message = message;
        this.downloadUrl = downloadUrl;
        this.packDownloadUrl = packDownloadUrl;
    }

    @Override
    protected void init()
    {
        maxWidth = Math.min(this.width - 60, 380);
        int lineGap = this.font.lineHeight + 2;
        int titleGap = 8;
        int textToButtonsGap = 16;
        int buttonGap = 8;
        int buttonH = 20;
        int buttonsBlock = buttonH * 2 + buttonGap;

        wrapped = this.font.split(this.message, maxWidth);
        int messageHeight = Math.max(lineGap, wrapped.size() * lineGap);

        int total = this.font.lineHeight + titleGap + messageHeight + textToButtonsGap + buttonsBlock;
        int top = Math.max(20, (this.height - total) / 2);

        titleY = top;
        messageY = titleY + this.font.lineHeight + titleGap;
        openBtnY = messageY + messageHeight + textToButtonsGap;
        quitBtnY = openBtnY + buttonH + buttonGap;

        int centerX = this.width / 2;
        int btnW = 220;

        this.clearWidgets();

        String buttonHref = (packDownloadUrl != null && !packDownloadUrl.isBlank())
                ? packDownloadUrl
                : downloadUrl;

        this.addRenderableWidget(Button.builder(Component.literal("Open Download Page"), b ->
        {
            try
            {
                Util.getPlatform().openUri(new URI(buttonHref));
            }
            catch
            (Exception ignored)
            {

            }
        }).bounds(centerX - btnW / 2, openBtnY, btnW, buttonH).build());

        this.addRenderableWidget(Button.builder(Component.literal("Quit Game"), b ->
        {
            Minecraft.getInstance().stop();
        }).bounds(centerX - btnW / 2, quitBtnY, btnW, buttonH).build());
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(gg);

        // Title
        gg.drawCenteredString(this.font, this.getTitle(), this.width / 2, titleY, 0xFFFFFF);

        // Wrapped message
        int y = messageY;
        int centerX = this.width / 2;
        for (FormattedCharSequence line : wrapped)
        {
            gg.drawCenteredString(this.font, line, centerX, y, 0xDDDDDD);
            y += this.font.lineHeight + 2;
        }

        super.render(gg, mouseX, mouseY, partialTick);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() { /* keep modal */ }
}
