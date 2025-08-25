package com.mesabrook.rzcraft;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.Logging;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Main.MODID)
public class Main 
{
	public static final String MODID = "packgate";

    public static String modpackName;

    // You can still keep a fallback timeout constant if you like
    public static final int REQUEST_TIMEOUT_SECONDS = 5;

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public Main()
    {
        // Register config first
        Config.register();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(final FMLClientSetupEvent event)
    {
        if(Config.MODPACK_NAME != null)
        {
            modpackName = Config.MODPACK_NAME.get();
        }
        else
        {
            modpackName = "Unnamed Modpack";
        }

        event.enqueueWork(() ->
        {
            String cfgVersion = Config.MODPACK_VERSION.get();
            final String currentVersion = (cfgVersion != null && !cfgVersion.isBlank())
                    ? cfgVersion.trim()
                    : ModList.get().getModContainerById(MODID)
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("0.0.0");

            final String remoteUrl = Config.REMOTE_URL.get();
            final String packDownloadUrl = Config.DOWNLOAD_URL.get();
            LOGGER.info("Boot check: current='{}', remoteURL='{}', downloadURL(cfg)='{}'",
                    currentVersion, remoteUrl, packDownloadUrl);
            VersionCheck.start(remoteUrl, packDownloadUrl, currentVersion, REQUEST_TIMEOUT_SECONDS);
        });
    }
}
